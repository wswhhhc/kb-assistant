import logging
import json
from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from app.schemas.chat import AskRequest, AskResponse
from app.services.qa_service import QAService

logger = logging.getLogger(__name__)
router = APIRouter()
qa_service = QAService()


@router.post("/ask", response_model=AskResponse)
async def ask_question(request: AskRequest):
    logger.info("收到问答请求: question=%s, kb=%s", request.question[:50], request.knowledgeBaseId)
    try:
        history = [{"role": m.role, "content": m.content} for m in request.history]
        result = await qa_service.ask(
            question=request.question,
            knowledge_base_id=request.knowledgeBaseId,
            session_id=request.sessionId,
            history=history
        )
        logger.info("问答完成: success=%s, citations=%s", result["success"], len(result["citations"]))
        return AskResponse(
            answer=result["answer"],
            citations=result["citations"],
            retrievalCount=result["retrievalCount"],
            modelName=result["modelName"],
            success=result["success"],
            message=""
        )
    except Exception as e:
        logger.error("问答失败", exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/ask-stream")
async def ask_question_stream(request: AskRequest):
    logger.info("收到流式问答: question=%s, kb=%s", request.question[:50], request.knowledgeBaseId)
    history = [{"role": m.role, "content": m.content} for m in request.history]

    async def event_stream():
        prepared = await qa_service.prepare_answer(
            question=request.question,
            knowledge_base_id=request.knowledgeBaseId,
            history=history,
        )

        if not prepared["success"]:
            yield f"event: done\ndata: {json.dumps({'answer': prepared['answer'], 'citations': [], 'retrievalCount': 0, 'modelName': '', 'success': False})}\n\n"
            return

        citations = prepared["citations"]
        yield f"event: citations\ndata: {json.dumps(citations)}\n\n"

        if not prepared["prompt"]:
            yield f"event: done\ndata: {json.dumps({'answer': prepared['answer'], 'citations': citations, 'retrievalCount': prepared['retrievalCount'], 'modelName': prepared['modelName'], 'success': True})}\n\n"
            return

        prompt = prepared["prompt"]
        full_answer = ""
        success = True

        try:
            async for msg_type, text in qa_service.generator.generate_stream(prompt):
                if msg_type == "thinking":
                    yield f"event: thinking\ndata: {json.dumps({'text': text})}\n\n"
                elif msg_type == "answer":
                    full_answer += text
                    yield f"event: answer\ndata: {json.dumps({'text': text})}\n\n"
        except Exception as e:
            logger.error("流式生成失败", exc_info=True)
            success = False
            if not full_answer:
                full_answer = "抱歉，答案生成失败，请稍后重试。"

        yield f"event: done\ndata: {json.dumps({'answer': full_answer, 'citations': citations, 'retrievalCount': prepared['retrievalCount'], 'modelName': prepared['modelName'], 'success': success})}\n\n"

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no"
        }
    )
