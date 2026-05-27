"""问答生成服务：拼接 Prompt + 调用 DeepSeek 生成答案"""

from typing import Optional
import httpx
from app.config.settings import settings


class QAGenerator:
    """问答生成器"""

    def __init__(self):
        self.api_key = settings.SILICONFLOW_API_KEY
        self.base_url = settings.SILICONFLOW_BASE_URL
        self.model = settings.CHAT_MODEL
        self.max_tokens = settings.MAX_TOKENS
        self.temperature = settings.TEMPERATURE

    def build_prompt(self, question: str, context: list[str]) -> str:
        """构建 Prompt"""
        context_text = "\n\n".join([f"[片段 {i+1}]: {c}" for i, c in enumerate(context)])

        prompt = f"""你是一个企业知识库智能助手。请根据以下提供的知识片段回答用户的问题。

知识片段：
{context_text}

用户问题：{question}

回答要求：
1. 请优先基于知识片段内容回答
2. 如果知识片段不足以回答问题，请明确说明"根据现有知识无法确认"
3. 不要编造不存在的信息
4. 请简明扼要地回答
"""
        return prompt

    async def generate(self, prompt: str) -> Optional[str]:
        """调用 DeepSeek 生成回答"""
        if not self.api_key:
            raise ValueError("SILICONFLOW_API_KEY 未配置")

        url = f"{self.base_url}/chat/completions"
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json"
        }
        payload = {
            "model": self.model,
            "messages": [{"role": "user", "content": prompt}],
            "max_tokens": self.max_tokens,
            "temperature": self.temperature
        }

        async with httpx.AsyncClient(timeout=60) as client:
            response = await client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            data = response.json()
            return data["choices"][0]["message"]["content"]
