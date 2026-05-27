from abc import ABC, abstractmethod
from typing import Optional


class ParseResult:
    """标准化解析结果"""

    def __init__(self, document_id: int, content: str,
                 section_title: Optional[str] = None,
                 page_no: Optional[int] = None):
        self.document_id = document_id
        self.section_title = section_title
        self.page_no = page_no
        self.content = content


class BaseParser(ABC):
    """解析器基类"""

    @abstractmethod
    def parse(self, file_path: str, document_id: int) -> list[ParseResult]:
        """解析文件，返回标准化结果列表"""
        pass
