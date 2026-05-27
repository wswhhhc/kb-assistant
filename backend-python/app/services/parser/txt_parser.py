from app.services.parser.base import BaseParser, ParseResult


class TxtParser(BaseParser):
    """TXT 解析器"""

    def parse(self, file_path: str, document_id: int) -> list[ParseResult]:
        with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
            text = f.read()

        results = []
        paragraphs = text.split("\n\n")
        for para in paragraphs:
            stripped = para.strip()
            if stripped:
                results.append(ParseResult(
                    document_id=document_id,
                    content=stripped
                ))
        return results
