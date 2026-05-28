from contextlib import asynccontextmanager
from fastapi import FastAPI
from qdrant_client import QdrantClient
from qdrant_client.models import VectorParams, Distance
from app.config.settings import settings
from app.routers import document, chat


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 启动时初始化 Qdrant collection（如果不存在）
    qdrant = QdrantClient(host=settings.QDRANT_HOST, port=settings.QDRANT_PORT)
    collections = qdrant.get_collections().collections
    names = [c.name for c in collections]
    if settings.QDRANT_COLLECTION not in names:
        qdrant.create_collection(
            collection_name=settings.QDRANT_COLLECTION,
            vectors_config=VectorParams(
                size=settings.EMBEDDING_DIMENSION,
                distance=Distance.COSINE
            )
        )
    yield


app = FastAPI(
    title=settings.APP_NAME,
    version="1.0.0",
    description="企业知识库智能助手 - AI 服务",
    lifespan=lifespan
)

app.include_router(document.router, prefix="/internal/document", tags=["document"])
app.include_router(chat.router, prefix="/internal/chat", tags=["chat"])


@app.get("/health")
def health_check():
    return {"status": "ok", "service": settings.APP_NAME}
