"""向量检索服务：在 Qdrant 中按知识库 ID 过滤检索"""

from typing import Optional
from qdrant_client import QdrantClient
from qdrant_client.models import Filter, FieldCondition, MatchValue
from app.config.settings import settings


class RetrievedChunk:
    def __init__(self, chunk_id: int, document_id: int,
                 knowledge_base_id: int, chunk_index: int,
                 content: str, file_name: str,
                 page_no: Optional[int], score: float):
        self.chunk_id = chunk_id
        self.document_id = document_id
        self.knowledge_base_id = knowledge_base_id
        self.chunk_index = chunk_index
        self.content = content
        self.file_name = file_name
        self.page_no = page_no
        self.score = score


class Retriever:
    """向量检索器"""

    def __init__(self):
        self.client = QdrantClient(
            host=settings.QDRANT_HOST,
            port=settings.QDRANT_PORT
        )
        self.collection = settings.QDRANT_COLLECTION
        self.top_k = settings.TOP_K

    def search(self, vector: list[float],
               knowledge_base_id: int) -> list[RetrievedChunk]:
        """按 knowledge_base_id 过滤检索"""
        filter_cond = Filter(
            must=[
                FieldCondition(
                    key="knowledge_base_id",
                    match=MatchValue(value=knowledge_base_id)
                )
            ]
        )

        results = self.client.search(
            collection_name=self.collection,
            query_vector=vector,
            query_filter=filter_cond,
            limit=self.top_k
        )

        chunks = []
        for point in results:
            payload = point.payload or {}
            chunks.append(RetrievedChunk(
                chunk_id=payload.get("chunk_id"),
                document_id=payload.get("document_id"),
                knowledge_base_id=payload.get("knowledge_base_id"),
                chunk_index=payload.get("chunk_index"),
                content=payload.get("content_preview", ""),
                file_name=payload.get("file_name", ""),
                page_no=payload.get("page_no"),
                score=point.score
            ))
        return chunks
