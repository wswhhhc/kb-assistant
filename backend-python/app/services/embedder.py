"""Embedding 服务：调用 SiliconFlow API 生成向量"""

from typing import Optional
from app.services.siliconflow_client import SiliconFlowClient


class Embedder:
    """向量化服务"""

    def __init__(self, client: Optional[SiliconFlowClient] = None):
        self.client = client or SiliconFlowClient()

    async def embed(self, text: str) -> Optional[list[float]]:
        return await self.client.embed(text)

    async def embed_batch(self, texts: list[str]) -> list[list[float]]:
        return await self.client.embed_batch(texts)
