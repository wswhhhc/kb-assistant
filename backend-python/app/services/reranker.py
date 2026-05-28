"""重排服务：调用 SiliconFlow Rerank API 对召回结果二次排序"""

from typing import Optional
import httpx
from app.config.settings import settings
from app.services.retriever import RetrievedChunk


class Reranker:
    """重排器：对检索结果进行精排，提升顶部结果相关性"""

    def __init__(self):
        self.api_key = settings.SILICONFLOW_API_KEY
        self.base_url = settings.SILICONFLOW_BASE_URL
        self.model = settings.RERANK_MODEL
        self.top_k = settings.RERANK_TOP_K

    async def rerank(self, question: str,
                     chunks: list[RetrievedChunk]) -> list[RetrievedChunk]:
        """对检索结果进行重排，返回重排后的 top_k 结果"""
        if not chunks or len(chunks) <= 1:
            return chunks

        if not self.api_key:
            return chunks[:self.top_k]

        documents = [c.content or "" for c in chunks]

        try:
            scored = await self._call_rerank_api(question, documents)
        except Exception:
            # 重排失败时回退到原始排序
            return chunks[:self.top_k]

        if not scored:
            return chunks[:self.top_k]

        # 按重排分数排序
        scored.sort(key=lambda x: x[1], reverse=True)

        reranked = []
        for idx, score in scored[:self.top_k]:
            chunk = chunks[idx]
            chunk.rerank_score = score
            chunk.score = score  # 用重排分数覆盖原始分数
            reranked.append(chunk)

        return reranked

    async def _call_rerank_api(self, query: str,
                               documents: list[str]) -> Optional[list[tuple[int, float]]]:
        """调用 SiliconFlow rerank API"""
        url = f"{self.base_url}/rerank"
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
        payload = {
            "model": self.model,
            "query": query,
            "documents": documents,
            "top_n": len(documents)
        }

        async with httpx.AsyncClient(timeout=30) as client:
            response = await client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            data = response.json()

            results = data.get("results", [])
            return [(item["index"], item["relevance_score"]) for item in results]
