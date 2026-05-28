"""文档处理编排服务：解析 → 切片 → 向量化 → 入库"""

import os
from typing import Optional
from qdrant_client import QdrantClient
from qdrant_client.models import VectorParams, Distance, PointStruct
from app.config.settings import settings, mysql_pool
from app.services.parser import PdfParser, DocxParser, TxtParser, MdParser
from app.services.chunker import Chunker
from app.services.embedder import Embedder


class DocumentProcessor:

    def __init__(self):
        self.chunker = Chunker(
            chunk_size=settings.CHUNK_SIZE,
            chunk_overlap=settings.CHUNK_OVERLAP
        )
        self.embedder = Embedder()
        self.qdrant = QdrantClient(
            host=settings.QDRANT_HOST,
            port=settings.QDRANT_PORT
        )
        self._ensure_collection()

    def _ensure_collection(self):
        collections = self.qdrant.get_collections().collections
        names = [c.name for c in collections]
        if settings.QDRANT_COLLECTION not in names:
            self.qdrant.create_collection(
                collection_name=settings.QDRANT_COLLECTION,
                vectors_config=VectorParams(
                    size=settings.EMBEDDING_DIMENSION,
                    distance=Distance.COSINE
                )
            )

    def _get_parser(self, file_type: str):
        parsers = {
            "PDF": PdfParser(),
            "DOCX": DocxParser(),
            "MD": MdParser(),
            "TXT": TxtParser(),
        }
        parser = parsers.get(file_type.upper())
        if not parser:
            raise ValueError(f"不支持的文件类型: {file_type}")
        return parser

    def _get_conn(self):
        return mysql_pool.connection()

    def _update_status(self, doc_id: int, status: str,
                       chunk_count: int = 0,
                       error_message: Optional[str] = None):
        conn = self._get_conn()
        try:
            with conn.cursor() as cursor:
                if status == "READY":
                    cursor.execute(
                        "UPDATE document SET parse_status=%s, chunk_count=%s, "
                        "updated_at=NOW() WHERE id=%s",
                        (status, chunk_count, doc_id)
                    )
                elif status == "FAILED":
                    cursor.execute(
                        "UPDATE document SET parse_status=%s, error_message=%s, "
                        "updated_at=NOW() WHERE id=%s",
                        (status, error_message, doc_id)
                    )
                else:
                    cursor.execute(
                        "UPDATE document SET parse_status=%s, "
                        "updated_at=NOW() WHERE id=%s",
                        (status, doc_id)
                    )
                conn.commit()
        finally:
            conn.close()

    async def process(self, document_id: int, knowledge_base_id: int,
                      file_path: str, file_name: str, file_type: str) -> dict:
        self._update_status(document_id, "PARSING")

        try:
            parser = self._get_parser(file_type)
            if not os.path.exists(file_path):
                raise FileNotFoundError(f"文件不存在: {file_path}")

            parse_results = parser.parse(file_path, document_id)

            self._update_status(document_id, "CHUNKING")

            chunks = self.chunker.chunk(document_id, knowledge_base_id, parse_results)
            if not chunks:
                raise ValueError("文档解析后未产生有效内容")

            self._update_status(document_id, "EMBEDDING")

            texts = [c.content for c in chunks]
            vectors = await self.embedder.embed_batch(texts)

            if len(vectors) != len(chunks):
                raise ValueError(
                    f"向量化结果数量不匹配: {len(vectors)} vs {len(chunks)}"
                )

            # 写入 Qdrant
            points = []
            for i, chunk in enumerate(chunks):
                point_id = document_id * 10_000_000 + chunk.chunk_index
                points.append(PointStruct(
                    id=point_id,
                    vector=vectors[i],
                    payload={
                        "document_id": document_id,
                        "knowledge_base_id": knowledge_base_id,
                        "chunk_index": chunk.chunk_index,
                        "content_preview": chunk.content[:200],
                        "file_name": file_name,
                        "page_no": chunk.page_no,
                        "section_title": chunk.section_title,
                    }
                ))

            self.qdrant.upsert(
                collection_name=settings.QDRANT_COLLECTION,
                points=points
            )

            # 写入 MySQL chunk 元数据
            conn = self._get_conn()
            try:
                with conn.cursor() as cursor:
                    for i, chunk in enumerate(chunks):
                        vector_id = str(document_id * 10_000_000 + chunk.chunk_index)
                        cursor.execute(
                            """INSERT INTO document_chunk
                               (document_id, knowledge_base_id, chunk_index,
                                section_title, content, page_no, char_count,
                                token_count, vector_id, created_at)
                               VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())""",
                            (document_id, knowledge_base_id, chunk.chunk_index,
                             chunk.section_title, chunk.content, chunk.page_no,
                             chunk.char_count, chunk.token_count, vector_id)
                        )
                    conn.commit()
            finally:
                conn.close()

            self._update_status(document_id, "READY", chunk_count=len(chunks))

            return {
                "success": True,
                "status": "READY",
                "chunkCount": len(chunks),
                "message": "文档处理完成"
            }

        except Exception as e:
            self._update_status(document_id, "FAILED", error_message=str(e))
            return {
                "success": False,
                "status": "FAILED",
                "chunkCount": 0,
                "message": str(e)
            }
