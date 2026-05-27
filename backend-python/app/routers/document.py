from fastapi import APIRouter
from app.schemas.document import ProcessRequest, ProcessResponse

router = APIRouter()


@router.post("/process", response_model=ProcessResponse)
async def process_document(request: ProcessRequest):
    """处理文档：解析 → 切片 → 向量化 → 入库"""
    # 将在第三阶段实现
    return ProcessResponse(
        success=True,
        status="PENDING",
        chunkCount=0,
        message="文档处理接口已就绪（待实现）"
    )
