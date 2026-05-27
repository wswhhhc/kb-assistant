"""Embedding 服务：调用 SiliconFlow API 生成向量"""

from typing import Optional
import httpx
from app.config.settings import settings


class Embedder:
    """向量化服务"""

    def __init__(self):
        self.api_key = settings.SILICONFLOW_API_KEY
        self.base_url = settings.SILICONFLOW_BASE_URL
        self.model = settings.EMBEDDING_MODEL

    async def embed(self, text: str) -> Optional[list[float]]:
        """生成文本向量"""
        if not self.api_key:
            raise ValueError("SILICONFLOW_API_KEY 未配置")

        url = f"{self.base_url}/embeddings"
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
        payload = {
            "model": self.model,
            "input": text
        }

        async with httpx.AsyncClient(timeout=30) as client:
            response = await client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            data = response.json()
            return data["data"][0]["embedding"]

    async def embed_batch(self, texts: list[str]) -> list[list[float]]:
        """批量生成向量"""
        results = []
        for text in texts:
            vector = await self.embed(text)
            if vector:
                results.append(vector)
        return results
