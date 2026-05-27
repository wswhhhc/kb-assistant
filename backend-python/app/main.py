from fastapi import FastAPI
from app.config.settings import settings
from app.routers import document, chat

app = FastAPI(
    title=settings.APP_NAME,
    version="1.0.0",
    description="企业知识库智能助手 - AI 服务"
)

app.include_router(document.router, prefix="/internal/document", tags=["document"])
app.include_router(chat.router, prefix="/internal/chat", tags=["chat"])


@app.get("/health")
def health_check():
    return {"status": "ok", "service": settings.APP_NAME}
