from docx import Document
from app.services.parser.base import BaseParser, ParseResult


class DocxParser(BaseParser):
    """DOCX 解析器（提取段落结构）"""

    def parse(self, file_path: str, document_id: int) -> list[ParseResult]:
        doc = Document(file_path)
        results = []
        current_section = None
        current_paras = []

        for para in doc.paragraphs:
            text = para.text.strip()
            if not text:
                continue

            if para.style.name.startswith("Heading"):
                if current_paras:
                    content = "\n".join(current_paras)
                    results.append(ParseResult(
                        document_id=document_id,
                        section_title=current_section,
                        content=content
                    ))
                    current_paras = []
                current_section = text
            else:
                current_paras.append(text)

        if current_paras:
            content = "\n".join(current_paras)
            results.append(ParseResult(
                document_id=document_id,
                section_title=current_section,
                content=content
            ))

        return results
