from fastapi import APIRouter
from app.schemas.chat import AskRequest, AskResponse

router = APIRouter()


@router.post("/ask", response_model=AskResponse)
async def ask_question(request: AskRequest):
    """问答：检索 → 拼接 → 生成 → 返回"""
    # 将在第四阶段实现
    return AskResponse(
        answer="",
        citations=[],
        retrievalCount=0,
        modelName="",
        success=False,
        message="问答接口已就绪（待实现）"
    )
