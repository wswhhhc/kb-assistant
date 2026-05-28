"""SiliconFlow API 统一客户端：封装 Embedding / Chat / Rerank 调用"""

from typing import Optional, AsyncGenerator
import httpx
import json as jsonlib
from app.config.settings import settings


class SiliconFlowClient:
    """SiliconFlow API 统一客户端"""

    def __init__(self):
        self.api_key = settings.SILICONFLOW_API_KEY
        self.base_url = settings.SILICONFLOW_BASE_URL

    def _headers(self, accept: str = "application/json") -> dict:
        return {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
            "Accept": accept,
        }

    async def _post(self, path: str, payload: dict, timeout: int = 60) -> dict:
        if not self.api_key:
            raise ValueError("SILICONFLOW_API_KEY 未配置")
        url = f"{self.base_url}{path}"
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.post(url, json=payload, headers=self._headers())
            response.raise_for_status()
            return response.json()

    async def _post_stream(
        self, path: str, payload: dict, timeout: int = 120
    ) -> AsyncGenerator[dict, None]:
        if not self.api_key:
            raise ValueError("SILICONFLOW_API_KEY 未配置")
        url = f"{self.base_url}{path}"
        headers = self._headers("text/event-stream")
        async with httpx.AsyncClient(timeout=timeout) as client:
            async with client.stream("POST", url, json=payload, headers=headers) as response:
                response.raise_for_status()
                async for line in response.aiter_lines():
                    if line.startswith("data: "):
                        data_str = line[6:]
                        if data_str.strip() == "[DONE]":
                            break
                        try:
                            yield jsonlib.loads(data_str)
                        except jsonlib.JSONDecodeError:
                            continue

    # --- Embedding ---

    async def embed(self, text: str) -> Optional[list[float]]:
        data = await self._post("/embeddings", {
            "model": settings.EMBEDDING_MODEL,
            "input": text,
        }, timeout=30)
        return data["data"][0]["embedding"]

    async def embed_batch(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        data = await self._post("/embeddings", {
            "model": settings.EMBEDDING_MODEL,
            "input": texts,
        }, timeout=60)
        return [item["embedding"] for item in data["data"]]

    # --- Chat Completion ---

    async def chat(self, prompt: str, stream: bool = False) -> Optional[str]:
        payload = {
            "model": settings.CHAT_MODEL,
            "messages": [{"role": "user", "content": prompt}],
            "max_tokens": settings.MAX_TOKENS,
            "temperature": settings.TEMPERATURE,
            "stream": stream,
        }
        data = await self._post("/chat/completions", payload, timeout=60)
        return data["choices"][0]["message"]["content"]

    async def chat_stream(
        self, prompt: str
    ) -> AsyncGenerator[tuple[str, str], None]:
        """逐 chunk 产出 (type, content) 二元组：type = "thinking" | "answer" """
        payload = {
            "model": settings.CHAT_MODEL,
            "messages": [{"role": "user", "content": prompt}],
            "max_tokens": settings.MAX_TOKENS,
            "temperature": settings.TEMPERATURE,
            "stream": True,
        }
        async for chunk in self._post_stream("/chat/completions", payload, timeout=120):
            delta = chunk.get("choices", [{}])[0].get("delta", {})
            reasoning = delta.get("reasoning_content", "")
            content = delta.get("content", "")
            if reasoning:
                yield ("thinking", reasoning)
            if content:
                yield ("answer", content)

    # --- Rerank ---

    async def rerank(self, query: str, documents: list[str], top_n: Optional[int] = None) -> list[tuple[int, float]]:
        payload = {
            "model": settings.RERANK_MODEL,
            "query": query,
            "documents": documents,
            "top_n": top_n or len(documents),
        }
        data = await self._post("/rerank", payload, timeout=30)
        results = data.get("results", [])
        return [(item["index"], item["relevance_score"]) for item in results]
