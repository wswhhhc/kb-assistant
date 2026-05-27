import os


def ensure_dir(path: str):
    """确保目录存在"""
    os.makedirs(path, exist_ok=True)


def get_file_extension(filename: str) -> str:
    """获取文件扩展名（大写）"""
    _, ext = os.path.splitext(filename)
    return ext[1:].upper() if ext else ""
