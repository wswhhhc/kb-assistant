from pydantic_settings import BaseSettings
from dbutils.pooled_db import PooledDB
import pymysql


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
    MYSQL_POOL_SIZE: int = 5      # 连接池大小
    MYSQL_POOL_MAX: int = 10      # 连接池最大连接数

    # 文档处理
    CHUNK_SIZE: int = 500
    CHUNK_OVERLAP: int = 50
    TOP_K: int = 5
    MAX_TOKENS: int = 2048
    TEMPERATURE: float = 0.3

    # Hybrid Search 配置
    ENABLE_HYBRID_SEARCH: bool = True
    KEYWORD_TOP_K: int = 5
    RRF_K: int = 60
    KEYWORD_WEIGHT: float = 0.4
    VECTOR_WEIGHT: float = 0.6

    # Rerank 配置
    ENABLE_RERANK: bool = True
    RERANK_MODEL: str = "BAAI/bge-reranker-v2-m3"
    RERANK_TOP_K: int = 5

    # 共享文件卷路径
    FILE_UPLOAD_DIR: str = "./data/files"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


settings = Settings()

# 全局 MySQL 连接池
mysql_pool = PooledDB(
    creator=pymysql,
    maxconnections=settings.MYSQL_POOL_MAX,
    mincached=settings.MYSQL_POOL_SIZE,
    blocking=True,
    host=settings.MYSQL_HOST,
    port=settings.MYSQL_PORT,
    user=settings.MYSQL_USER,
    password=settings.MYSQL_PASSWORD,
    database=settings.MYSQL_DATABASE,
    charset="utf8mb4",
    cursorclass=pymysql.cursors.Cursor
)
