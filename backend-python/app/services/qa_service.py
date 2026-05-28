"""问答编排服务：向量化 → 混合检索 → 重排 → 生成 → 引用"""

from typing import Optional
from app.config.settings import settings
from app.services.embedder import Embedder
from app.services.retriever import Retriever
from app.services.reranker import Reranker
from app.services.qa_generator import QAGenerator
from app.services.citation import CitationBuilder
from app.services.siliconflow_client import SiliconFlowClient


class QAService:

    def __init__(self, client: Optional[SiliconFlowClient] = None):
        self.client = client or SiliconFlowClient()
        self.embedder = Embedder(self.client)
        self.retriever = Retriever()
        self.reranker = Reranker(self.client)
        self.generator = QAGenerator(self.client)
        self.citation_builder = CitationBuilder()

    @staticmethod
    def _normalize_question(question: str) -> str:
        normalized = question.strip()
        for token in ["请问", "一下", "吗", "么", "呢", "是否", "是不是"]:
            normalized = normalized.replace(token, "")
        return " ".join(normalized.split())

    def _build_query_variants(self, question: str) -> list[str]:
        variants = []

        def add(text: str):
            text = text.strip()
            if text and text not in variants:
                variants.append(text)

        add(question)
        normalized = self._normalize_question(question)
        if normalized != question:
            add(normalized)
        add(normalized.replace("公司", ""))

        return variants[:5]

    @staticmethod
    def _keyword_overlap_score(question: str, content: str) -> float:
        question_words = set(token for token in question if len(token) > 1)
        if not question_words:
            return 0.0
        content_words = set(token for token in content if len(token) > 1)
        overlap = len(question_words & content_words)
        return overlap * 0.05 if overlap > 0 else 0.0

    def _merge_retrieved_chunks(self, question: str, retrieved_groups: list[list]) -> list:
        merged = {}
        for chunks in retrieved_groups:
            for chunk in chunks:
                key = (chunk.document_id, chunk.chunk_index)
                existing = merged.get(key)
                if existing is None:
                    merged[key] = chunk
                    continue

                existing.score = max(existing.score, chunk.score)
                existing.match_count += 1
                if len(chunk.content or "") > len(existing.content or ""):
                    existing.content = chunk.content

        ranked_chunks = list(merged.values())
        for chunk in ranked_chunks:
            chunk.score = (
                chunk.score
                + min(chunk.match_count - 1, 3) * 0.05
                + self._keyword_overlap_score(question, chunk.content or "")
            )

        ranked_chunks.sort(key=lambda item: item.score, reverse=True)
        return ranked_chunks[:self.retriever.top_k]

    async def retrieve_context(self, question: str, knowledge_base_id: int) -> dict:
        query_variants = self._build_query_variants(question)
        retrieved_groups = []

        for query in query_variants:
            vector = await self.embedder.embed(query)
            if not vector:
                continue

            if settings.ENABLE_HYBRID_SEARCH:
                chunks = self.retriever.hybrid_search(vector, query, knowledge_base_id)
            else:
                chunks = self.retriever.search(vector, knowledge_base_id)

            retrieved_groups.append(chunks)

        if not retrieved_groups:
            return {
                "answer": "问题向量化失败",
                "chunks": [],
                "citations": [],
                "retrievalCount": 0,
                "modelName": "",
                "success": False
            }

        chunks = self._merge_retrieved_chunks(question, retrieved_groups)
        if not chunks:
            return {
                "answer": "知识库中未找到相关内容，请尝试换个问题或上传更多文档。",
                "chunks": [],
                "citations": [],
                "retrievalCount": 0,
                "modelName": "",
                "success": True
            }

        if settings.ENABLE_RERANK and len(chunks) > 1:
            chunks = await self.reranker.rerank(question, chunks)

        citations = self.citation_builder.build_from_chunks(chunks)

        return {
            "answer": "",
            "chunks": chunks,
            "citations": citations,
            "retrievalCount": len(chunks),
            "modelName": settings.CHAT_MODEL,
            "success": True
        }

    async def prepare_answer(self, question: str, knowledge_base_id: int,
                             history: Optional[list[dict]] = None) -> dict:
        """准备问答上下文：查询改写 → 检索 → prompt 构建

        返回：
            success: bool
            retrieval_question: str  — 改写后的问题（用于检索）
            answer: str              — 检索失败时的错误信息
            chunks: list             — 检索到的片段
            contexts: list[str]      — 片段内容列表
            citations: list          — 引用列表
            retrievalCount: int
            modelName: str
            prompt: str              — 构建好的 Prompt（仅在 success=True 且 chunks 非空时有值）
        """
        history = history or []

        # 查询改写
        retrieval_question = question
        if history:
            rewritten = await self.generator.rewrite_query(question, history)
            if rewritten and rewritten != question:
                retrieval_question = rewritten

        # 检索
        retrieval_result = await self.retrieve_context(retrieval_question, knowledge_base_id)
        retrieval_question = retrieval_question or question

        result = {
            "success": retrieval_result["success"],
            "retrieval_question": retrieval_question,
            "answer": retrieval_result["answer"],
            "chunks": retrieval_result["chunks"],
            "citations": retrieval_result["citations"],
            "retrievalCount": retrieval_result["retrievalCount"],
            "modelName": retrieval_result["modelName"],
            "prompt": None,
            "contexts": [],
        }

        if not retrieval_result["success"] or not retrieval_result["chunks"]:
            return result

        contexts = [c.content for c in retrieval_result["chunks"]]
        result["contexts"] = contexts
        result["prompt"] = self.generator.build_context_prompt(question, contexts, history)

        return result

    async def ask(self, question: str, knowledge_base_id: int,
                  session_id: Optional[int] = None,
                  history: Optional[list[dict]] = None) -> dict:
        prepared = await self.prepare_answer(question, knowledge_base_id, history)

        if not prepared["success"] or not prepared["prompt"]:
            return {
                "answer": prepared["answer"] or "抱歉，无法生成回答。",
                "citations": prepared["citations"],
                "retrievalCount": prepared["retrievalCount"],
                "modelName": prepared["modelName"],
                "success": prepared["success"] if prepared["success"] else False,
            }

        answer = await self.generator.generate(prepared["prompt"])
        if not answer:
            return {
                "answer": "抱歉，答案生成失败，请稍后重试。",
                "citations": prepared["citations"],
                "retrievalCount": prepared["retrievalCount"],
                "modelName": settings.CHAT_MODEL,
                "success": False,
            }

        return {
            "answer": answer,
            "citations": prepared["citations"],
            "retrievalCount": prepared["retrievalCount"],
            "modelName": settings.CHAT_MODEL,
            "success": True,
        }
