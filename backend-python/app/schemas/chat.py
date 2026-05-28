from pydantic import BaseModel
from typing import Optional


class Citation(BaseModel):
    documentId: int
    fileName: str
    pageNo: Optional[int] = None
    chunkIndex: int
    contentPreview: str
    content: Optional[str] = ""


class HistoryMessage(BaseModel):
    role: str  # USER / AI
    content: str


class AskRequest(BaseModel):
    userId: int
    knowledgeBaseId: int
    sessionId: Optional[int] = None
    question: str
    history: list[HistoryMessage] = []  # 多轮会话历史


class AskResponse(BaseModel):
    answer: str
    citations: list[Citation] = []
    retrievalCount: int = 0
    modelName: str = ""
    success: bool
    message: str = ""
