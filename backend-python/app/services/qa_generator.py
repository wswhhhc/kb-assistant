"""问答生成服务：拼接 Prompt + 调用大模型生成答案"""

from typing import Optional, AsyncGenerator
import httpx
import json as jsonlib
from app.config.settings import settings


class QAGenerator:
    """问答生成器"""

    def __init__(self):
        self.api_key = settings.SILICONFLOW_API_KEY
        self.base_url = settings.SILICONFLOW_BASE_URL
        self.model = settings.CHAT_MODEL
        self.max_tokens = settings.MAX_TOKENS
        self.temperature = settings.TEMPERATURE

    HISTORY_TOKEN_BUDGET = 2000

    @staticmethod
    def _needs_rewrite(question: str, history: list[dict]) -> bool:
        """判断当前问题是否是省略式追问，是否需要改写"""
        q = question.strip()
        if not q:
            return False

        # 短问题（<5字）：大概率是追问
        if len(q) < 5:
            return True

        # 以指代词开头
        indicators = ["那", "它", "这个", "这些", "那些", "怎么样", "如何", "呢", "吧", "然后"]
        for ind in indicators:
            if q.startswith(ind):
                return True

        # 与上一条用户问题对比，如果完全无重叠则可能是新话题
        prev_user_msgs = [m["content"] for m in history if m["role"] == "USER"]
        if prev_user_msgs:
            prev = prev_user_msgs[-1]
            prev_words = set(prev)
            q_words = set(q)
            overlap = len(prev_words & q_words) / max(len(q_words), 1)
            if overlap < 0.1 and len(q_words) > 3:
                return False  # 完全不同的新话题，不改写

        return True

    @staticmethod
    def _format_history(history: list[dict]) -> str:
        """格式化历史消息，基于 token 预算动态截断"""
        if not history:
            return ""

        lines = []
        total_est_tokens = 0
        # 从最新消息往前遍历，保留最近 2 轮完整对话再按预算截断
        for msg in reversed(history):
            role = "用户" if msg["role"] == "USER" else "助手"
            text = f"{role}: {msg['content']}"
            est_tokens = len(text) / 2  # 中文估算
            if total_est_tokens + est_tokens > QAGenerator.HISTORY_TOKEN_BUDGET:
                break
            lines.append(text)
            total_est_tokens += est_tokens

        lines.reverse()
        return "\n".join(lines)

    async def rewrite_query(self, question: str,
                            history: list[dict]) -> str:
        """对省略式追问做查询改写，补全上下文"""
        if not history:
            return question

        # 非省略式追问直接跳过改写，节省一次 LLM 调用
        if not self._needs_rewrite(question, history):
            return question

        history_text = self._format_history(history)

        prompt = f"""你是一个智能问答系统的查询改写助手。用户正在与知识库助手进行多轮对话。

历史对话：
{history_text}

用户当前问题：{question}

任务：如果当前问题是省略式追问（省略了主语、缺少上下文、指代不明等），请结合历史对话将其改写成独立、完整的查询。如果当前问题本身已经是完整问题，则原样输出。

改写规则：
1. 补充省略的主语或宾语
2. 将指代词（它、这个、那个、这些等）替换为具体内容
3. 保持原问题的核心意图
4. 改写结果应该是一个独立的、可直接用于检索的问题
5. 不要添加原问题中不存在的信息
6. 如果问题本身就是完整的，直接原样输出

只输出改写后的问题，不要输出任何解释。"""

        rewritten = await self.generate(prompt)
        if rewritten:
            result = rewritten.strip()
            return result if result else question
        return question

    def build_context_prompt(self, question: str,
                             context: list[str],
                             history: list[dict]) -> str:
        """构建带历史上下文的 Prompt"""
        context_text = "\n\n".join([f"[片段 {i+1}]: {c}" for i, c in enumerate(context)])

        history_text = self._format_history(history) if history else ""

        history_section = f"""
历史对话：
{history_text}
""" if history_text else ""

        prompt = f"""你是一个企业知识库智能助手。请根据以下提供的知识片段和历史对话回答用户的问题。
{history_section}
知识片段：
{context_text}

用户问题：{question}

回答要求：
1. 请优先基于知识片段内容回答
2. 结合历史对话上下文理解用户当前问题
3. 如果知识片段不足以回答问题，请明确说明"根据现有知识无法确认"
4. 不要编造不存在的信息
5. 如果用户问题是"是否 / 是不是 / 吗"这类判断题，且知识片段可以支持判断，请先明确回答"是"或"否"，再补一句依据
6. 注意区分相近概念，不要把"午休时间"误答成"每日工作时长"等其他字段
7. 请简明扼要地回答
8. 引用要求：在回答中引用知识片段时，请在对应句子末尾标注片段编号，格式为 [N]，其中 N 为片段序号。例如：根据规定，午休时间为1.5小时[1]。多个引用用逗号分隔，例如：[1][2] 或 [1,2]。
"""
        return prompt

    async def generate(self, prompt: str) -> Optional[str]:
        """调用模型生成回答（非流式）"""
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
            "temperature": self.temperature,
            "stream": False
        }

        async with httpx.AsyncClient(timeout=60) as client:
            response = await client.post(url, json=payload, headers=headers)
            response.raise_for_status()
            data = response.json()
            return data["choices"][0]["message"]["content"]

    async def generate_stream(self, prompt: str) -> AsyncGenerator[tuple[str, str], None]:
        """调用模型生成回答（流式），逐 chunk 产出 (type, content) 二元组

        type 取值：
          - "thinking" — 模型推理过程（reasoning_content）
          - "answer"   — 最终回答内容（content）
        """
        if not self.api_key:
            raise ValueError("SILICONFLOW_API_KEY 未配置")

        url = f"{self.base_url}/chat/completions"
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
            "Accept": "text/event-stream"
        }
        payload = {
            "model": self.model,
            "messages": [{"role": "user", "content": prompt}],
            "max_tokens": self.max_tokens,
            "temperature": self.temperature,
            "stream": True
        }

        async with httpx.AsyncClient(timeout=120) as client:
            async with client.stream("POST", url, json=payload, headers=headers) as response:
                response.raise_for_status()
                async for line in response.aiter_lines():
                    if line.startswith("data: "):
                        data_str = line[6:]
                        if data_str.strip() == "[DONE]":
                            break
                        try:
                            chunk = jsonlib.loads(data_str)
                            delta = chunk.get("choices", [{}])[0].get("delta", {})
                            reasoning = delta.get("reasoning_content", "")
                            content = delta.get("content", "")
                            if reasoning:
                                yield ("thinking", reasoning)
                            if content:
                                yield ("answer", content)
                        except jsonlib.JSONDecodeError:
                            continue
