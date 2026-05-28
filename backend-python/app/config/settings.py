from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    APP_NAME: str = "KB Assistant AI Service"
    DEBUG: bool = False

    # SiliconFlow API 配置
    SILICONFLOW_API_KEY: str = ""
    SILICONFLOW_BASE_URL: str = "https://api.siliconflow.cn/v1"
    EMBEDDING_MODEL: str = "BAAI/bge-m3"
    EMBEDDING_DIMENSION: int = 1024
    CHAT_MODEL: str = "deepseek-ai/DeepSeek-V4-Flash"

    # Qdrant 配置
    QDRANT_HOST: str = "localhost"
    QDRANT_PORT: int = 6333
    QDRANT_COLLECTION: str = "kb_chunks"

    # MySQL 配置（AI 服务直连读 chunk 元数据）
    MYSQL_HOST: str = "localhost"
    MYSQL_PORT: int = 3306
    MYSQL_USER: str = "root"
    MYSQL_PASSWORD: str = "root"
    MYSQL_DATABASE: str = "knowledge_base_assistant"

    # 文档处理
    CHUNK_SIZE: int = 500
    CHUNK_OVERLAP: int = 50
    TOP_K: int = 5
    MAX_TOKENS: int = 2048
    TEMPERATURE: float = 0.3

    # Hybrid Search 配置
    ENABLE_HYBRID_SEARCH: bool = True
    KEYWORD_TOP_K: int = 5           # 关键词检索取回数量
    RRF_K: int = 60                   # RRF 融合常数
    KEYWORD_WEIGHT: float = 0.4      # 关键词检索权重
    VECTOR_WEIGHT: float = 0.6       # 向量检索权重

    # Rerank 配置
    ENABLE_RERANK: bool = True
    RERANK_MODEL: str = "BAAI/bge-reranker-v2-m3"
    RERANK_TOP_K: int = 5            # 重排后保留数量

    # 共享文件卷路径
    FILE_UPLOAD_DIR: str = "./data/files"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()
