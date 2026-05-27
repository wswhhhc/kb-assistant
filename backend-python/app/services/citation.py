"""引用溯源服务：将检索结果组装为前端可展示的引用信息"""

from typing import Optional


class Citation:
    def __init__(self, file_name: str, chunk_index: int,
                 content_preview: str, page_no: Optional[int] = None):
        self.fileName = file_name
        self.chunkIndex = chunk_index
        self.contentPreview = content_preview
        self.pageNo = page_no


class CitationBuilder:
    """引用组装器"""

    @staticmethod
    def build_from_chunks(chunks: list) -> list[dict]:
        """将检索结果组装为引用列表"""
        citations = []
        for chunk in chunks:
            citations.append({
                "fileName": chunk.file_name,
                "pageNo": chunk.page_no,
                "chunkIndex": chunk.chunk_index,
                "contentPreview": chunk.content[:200] if chunk.content else ""
            })
        return citations
