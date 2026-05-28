"""检索服务：向量检索 + 关键词检索 + RRF 融合"""

import math
from typing import Optional
import pymysql
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
        self.match_count = 1
        self.rerank_score: Optional[float] = None  # 重排后分数


class Retriever:
    """检索器：支持向量检索、关键词检索与混合检索"""

    def __init__(self):
        self.client = QdrantClient(
            host=settings.QDRANT_HOST,
            port=settings.QDRANT_PORT
        )
        self.collection = settings.QDRANT_COLLECTION
        self.top_k = settings.TOP_K

    @staticmethod
    def _get_mysql_conn():
        return pymysql.connect(
            host=settings.MYSQL_HOST,
            port=settings.MYSQL_PORT,
            user=settings.MYSQL_USER,
            password=settings.MYSQL_PASSWORD,
            database=settings.MYSQL_DATABASE,
            charset="utf8mb4"
        )

    def _fill_chunk_content(self, chunks: list[RetrievedChunk]) -> None:
        if not chunks:
            return

        keys = {(chunk.document_id, chunk.chunk_index) for chunk in chunks}
        placeholders = ",".join(["(%s,%s)"] * len(keys))
        sql = (
            "SELECT document_id, chunk_index, content "
            "FROM document_chunk "
            f"WHERE (document_id, chunk_index) IN ({placeholders})"
        )

        params = []
        for document_id, chunk_index in keys:
            params.extend([document_id, chunk_index])

        conn = self._get_mysql_conn()
        try:
            with conn.cursor() as cursor:
                cursor.execute(sql, params)
                content_map = {
                    (doc_id, idx): content
                    for doc_id, idx, content in cursor.fetchall()
                }
        finally:
            conn.close()

        for chunk in chunks:
            full_content = content_map.get((chunk.document_id, chunk.chunk_index))
            if full_content:
                chunk.content = full_content

    def search(self, vector: list[float],
               knowledge_base_id: int) -> list[RetrievedChunk]:
        """向量检索：按 knowledge_base_id 过滤"""
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
            limit=self.top_k * 2  # 多召回一些供混合时筛选
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
        self._fill_chunk_content(chunks)
        return chunks

    def keyword_search(self, question: str,
                       knowledge_base_id: int) -> list[RetrievedChunk]:
        """关键词检索：使用 MySQL FULLTEXT 搜索 document_chunk.content"""
        top_k = settings.KEYWORD_TOP_K * 2
        conn = self._get_mysql_conn()
        try:
            with conn.cursor() as cursor:
                sql = """
                    SELECT dc.document_id, dc.chunk_index, dc.content,
                           d.file_name, dc.page_no, d.id as doc_id,
                           MATCH(dc.content) AGAINST(%s IN NATURAL LANGUAGE MODE) AS score
                    FROM document_chunk dc
                    JOIN document d ON dc.document_id = d.id
                    WHERE d.knowledge_base_id = %s
                      AND MATCH(dc.content) AGAINST(%s IN NATURAL LANGUAGE MODE)
                    ORDER BY score DESC
                    LIMIT %s
                """
                cursor.execute(sql, (question, knowledge_base_id, question, top_k))
                rows = cursor.fetchall()
        finally:
            conn.close()

        chunks = []
        for row in rows:
            doc_id, chunk_index, content, file_name, page_no, _, score = row
            if score <= 0:
                continue

            chunk = RetrievedChunk(
                chunk_id=doc_id * 10_000_000 + chunk_index,
                document_id=doc_id,
                knowledge_base_id=knowledge_base_id,
                chunk_index=chunk_index,
                content=content,
                file_name=file_name,
                page_no=page_no,
                score=float(score)
            )
            chunks.append(chunk)

        return chunks

    @staticmethod
    def _rrf_score(rank: int, k: int = 60) -> float:
        """计算 RRF（Reciprocal Rank Fusion）分数"""
        return 1.0 / (k + rank + 1)

    def hybrid_search(self, vector: list[float],
                      question: str,
                      knowledge_base_id: int) -> list[RetrievedChunk]:
        """混合检索：向量检索 + 关键词检索 → RRF 融合"""
        vector_chunks = self.search(vector, knowledge_base_id)
        keyword_chunks = self.keyword_search(question, knowledge_base_id)

        if not vector_chunks and not keyword_chunks:
            return []
        if not vector_chunks:
            return keyword_chunks[:self.top_k]
        if not keyword_chunks:
            return vector_chunks[:self.top_k]

        rrf_k = settings.RRF_K
        vector_weight = settings.VECTOR_WEIGHT
        keyword_weight = settings.KEYWORD_WEIGHT

        # 对每个 list 按 score 排序，得到 rank
        vector_chunks.sort(key=lambda c: c.score, reverse=True)
        keyword_chunks.sort(key=lambda c: c.score, reverse=True)

        # 计算融合分数
        merged = {}

        for rank, chunk in enumerate(vector_chunks):
            key = (chunk.document_id, chunk.chunk_index)
            merged[key] = {
                "chunk": chunk,
                "score": self._rrf_score(rank, rrf_k) * vector_weight
            }

        for rank, chunk in enumerate(keyword_chunks):
            key = (chunk.document_id, chunk.chunk_index)
            if key in merged:
                merged[key]["score"] += self._rrf_score(rank, rrf_k) * keyword_weight
                # 合并内容，取更长的
                if len(chunk.content or "") > len(merged[key]["chunk"].content or ""):
                    merged[key]["chunk"].content = chunk.content
            else:
                merged[key] = {
                    "chunk": chunk,
                    "score": self._rrf_score(rank, rrf_k) * keyword_weight
                }

        result = [item["chunk"] for item in merged.values()]
        result.sort(key=lambda c: merged[(c.document_id, c.chunk_index)]["score"], reverse=True)
        for c in result:
            c.score = merged[(c.document_id, c.chunk_index)]["score"]

        return result[:self.top_k * 2]  # 多召回供 rerank 筛选
