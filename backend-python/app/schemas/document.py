from pydantic import BaseModel


class ProcessRequest(BaseModel):
    documentId: int
    knowledgeBaseId: int
    filePath: str
    fileName: str
    fileType: str


class ProcessResponse(BaseModel):
    success: bool
    status: str
    chunkCount: int
    message: str
