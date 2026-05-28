"""重排服务：调用 SiliconFlow Rerank API 对召回结果二次排序"""

from typing import Optional
from app.config.settings import settings
from app.services.siliconflow_client import SiliconFlowClient
from app.services.retriever import RetrievedChunk


class Reranker:
    """重排器：对检索结果进行精排，提升顶部结果相关性"""

    def __init__(self, client: Optional[SiliconFlowClient] = None):
        self.client = client or SiliconFlowClient()
        self.top_k = settings.RERANK_TOP_K

    async def rerank(self, question: str, chunks: list[RetrievedChunk]) -> list[RetrievedChunk]:
        if not chunks or len(chunks) <= 1:
            return chunks

        if not self.client.api_key:
            return chunks[:self.top_k]

        documents = [c.content or "" for c in chunks]

        try:
            scored = await self._call_rerank_api(question, documents)
        except Exception:
            return chunks[:self.top_k]

        if not scored:
            return chunks[:self.top_k]

        scored.sort(key=lambda x: x[1], reverse=True)

        reranked = []
        for idx, score in scored[:self.top_k]:
            chunk = chunks[idx]
            chunk.rerank_score = score
            chunk.score = score
            reranked.append(chunk)

        return reranked

    async def _call_rerank_api(self, query: str, documents: list[str]) -> Optional[list[tuple[int, float]]]:
        return await self.client.rerank(query, documents)
