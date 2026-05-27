import fitz  # PyMuPDF
from app.services.parser.base import BaseParser, ParseResult


class PdfParser(BaseParser):
    """PDF 解析器（提取正文 + 页码映射）"""

    def parse(self, file_path: str, document_id: int) -> list[ParseResult]:
        doc = fitz.open(file_path)
        results = []

        for page_num, page in enumerate(doc, start=1):
            text = page.get_text().strip()
            if text:
                results.append(ParseResult(
                    document_id=document_id,
                    content=text,
                    page_no=page_num
                ))

        doc.close()
        return results
