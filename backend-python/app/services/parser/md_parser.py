import re
from app.services.parser.base import BaseParser, ParseResult


class MdParser(BaseParser):
    """Markdown 解析器（保留标题层级）"""

    def parse(self, file_path: str, document_id: int) -> list[ParseResult]:
        with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
            text = f.read()

        results = []
        current_section = None
        current_lines = []
        lines = text.split("\n")

        for line in lines:
            heading_match = re.match(r"^(#{1,6})\s+(.+)$", line)
            if heading_match:
                if current_lines:
                    content = "\n".join(current_lines).strip()
                    if content:
                        results.append(ParseResult(
                            document_id=document_id,
                            section_title=current_section,
                            content=content
                        ))
                    current_lines = []
                current_section = heading_match.group(2)
            else:
                current_lines.append(line)

        if current_lines:
            content = "\n".join(current_lines).strip()
            if content:
                results.append(ParseResult(
                    document_id=document_id,
                    section_title=current_section,
                    content=content
                ))

        return results
