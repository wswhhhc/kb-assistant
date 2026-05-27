from pydantic import BaseModel
from typing import Optional


class Citation(BaseModel):
    fileName: str
    pageNo: Optional[int] = None
    chunkIndex: int
    contentPreview: str


class AskRequest(BaseModel):
    userId: int
    knowledgeBaseId: int
    sessionId: Optional[int] = None
    question: str


class AskResponse(BaseModel):
    answer: str
    citations: list[Citation] = []
    retrievalCount: int = 0
    modelName: str = ""
    success: bool
    message: str = ""
