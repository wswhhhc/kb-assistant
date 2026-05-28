"""问答编排服务：向量化 → 混合检索 → 重排 → 生成 → 引用"""

from typing import Optional
from app.config.settings import settings
from app.services.embedder import Embedder
from app.services.retriever import Retriever
from app.services.reranker import Reranker
from app.services.qa_generator import QAGenerator
from app.services.citation import CitationBuilder


class QAService:

    def __init__(self):
        self.embedder = Embedder()
        self.retriever = Retriever()
        self.reranker = Reranker()
        self.generator = QAGenerator()
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
        """基于 TF 的通用关键词重叠评分"""
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

        # 向量检索（现有逻辑）
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

        # Rerank 精排
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

    async def ask(self, question: str, knowledge_base_id: int,
                  session_id: Optional[int] = None,
                  history: Optional[list[dict]] = None) -> dict:
        history = history or []

        # 查询改写：对省略式追问补充上下文
        if history:
            rewritten = await self.generator.rewrite_query(question, history)
            retrieval_question = rewritten if rewritten and rewritten != question else question
        else:
            retrieval_question = question

        # 用改写后的问题进行检索
        retrieval_result = await self.retrieve_context(retrieval_question, knowledge_base_id)
        if not retrieval_result["success"] or not retrieval_result["chunks"]:
            return {
                "answer": retrieval_result["answer"],
                "citations": retrieval_result["citations"],
                "retrievalCount": retrieval_result["retrievalCount"],
                "modelName": retrieval_result["modelName"],
                "success": retrieval_result["success"]
            }

        contexts = [c.content for c in retrieval_result["chunks"]]

        # 使用带历史上下文的 prompt
        prompt = self.generator.build_context_prompt(question, contexts, history)

        answer = await self.generator.generate(prompt)
        if not answer:
            return {
                "answer": "抱歉，答案生成失败，请稍后重试。",
                "citations": retrieval_result["citations"],
                "retrievalCount": retrieval_result["retrievalCount"],
                "modelName": settings.CHAT_MODEL,
                "success": False
            }

        return {
            "answer": answer,
            "citations": retrieval_result["citations"],
            "retrievalCount": retrieval_result["retrievalCount"],
            "modelName": settings.CHAT_MODEL,
            "success": True
        }
