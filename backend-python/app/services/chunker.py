"""文本切片服务：优先按标题/段落切片，回退到固定长度切片"""

from typing import Optional


class Chunk:
    def __init__(self, document_id: int, knowledge_base_id: int,
                 chunk_index: int, content: str,
                 section_title: Optional[str] = None,
                 page_no: Optional[int] = None):
        self.document_id = document_id
        self.knowledge_base_id = knowledge_base_id
        self.chunk_index = chunk_index
        self.content = content
        self.section_title = section_title
        self.page_no = page_no
        self.char_count = len(content)
        self.token_count = len(content) // 2  # 粗略估算


class Chunker:
    """文本切分器"""

    def __init__(self, chunk_size: int = 500, chunk_overlap: int = 50):
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap

    def chunk(self, document_id: int, knowledge_base_id: int,
              parse_results: list) -> list[Chunk]:
        """将解析结果切片"""
        chunks = []
        chunk_index = 0

        for result in parse_results:
            text = result.content
            if not text:
                continue

            # 如果文本较短，直接作为一个 chunk
            if len(text) <= self.chunk_size:
                chunks.append(Chunk(
                    document_id=document_id,
                    knowledge_base_id=knowledge_base_id,
                    chunk_index=chunk_index,
                    content=text,
                    section_title=result.section_title,
                    page_no=result.page_no
                ))
                chunk_index += 1
            else:
                # 固定长度切片 + overlap
                start = 0
                while start < len(text):
                    end = start + self.chunk_size
                    segment = text[start:end]
                    chunks.append(Chunk(
                        document_id=document_id,
                        knowledge_base_id=knowledge_base_id,
                        chunk_index=chunk_index,
                        content=segment,
                        section_title=result.section_title,
                        page_no=result.page_no
                    ))
                    chunk_index += 1
                    start = end - self.chunk_overlap

        return chunks
