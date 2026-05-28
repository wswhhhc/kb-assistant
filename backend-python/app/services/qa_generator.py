"""问答生成服务：拼接 Prompt + 调用大模型生成答案"""

from typing import Optional, AsyncGenerator
from app.config.settings import settings
from app.services.siliconflow_client import SiliconFlowClient


class QAGenerator:
    """问答生成器"""

    def __init__(self, client: Optional[SiliconFlowClient] = None):
        self.client = client or SiliconFlowClient()

    HISTORY_TOKEN_BUDGET = 2000

    @staticmethod
    def _needs_rewrite(question: str, history: list[dict]) -> bool:
        q = question.strip()
        if not q:
            return False

        if len(q) < 5:
            return True

        indicators = ["那", "它", "这个", "这些", "那些", "怎么样", "如何", "呢", "吧", "然后"]
        for ind in indicators:
            if q.startswith(ind):
                return True

        prev_user_msgs = [m["content"] for m in history if m["role"] == "USER"]
        if prev_user_msgs:
            prev = prev_user_msgs[-1]
            prev_words = set(prev)
            q_words = set(q)
            overlap = len(prev_words & q_words) / max(len(q_words), 1)
            if overlap < 0.1 and len(q_words) > 3:
                return False

        return True

    @staticmethod
    def _format_history(history: list[dict]) -> str:
        if not history:
            return ""

        lines = []
        total_est_tokens = 0
        for msg in reversed(history):
            role = "用户" if msg["role"] == "USER" else "助手"
            text = f"{role}: {msg['content']}"
            est_tokens = len(text) / 2
            if total_est_tokens + est_tokens > QAGenerator.HISTORY_TOKEN_BUDGET:
                break
            lines.append(text)
            total_est_tokens += est_tokens

        lines.reverse()
        return "\n".join(lines)

    async def rewrite_query(self, question: str, history: list[dict]) -> str:
        if not history:
            return question

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

        rewritten = await self.client.chat(prompt)
        if rewritten:
            result = rewritten.strip()
            return result if result else question
        return question

    def build_context_prompt(self, question: str, context: list[str], history: list[dict]) -> str:
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
        return await self.client.chat(prompt)

    async def generate_stream(self, prompt: str) -> AsyncGenerator[tuple[str, str], None]:
        async for item in self.client.chat_stream(prompt):
            yield item
