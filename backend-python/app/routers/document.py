import logging
from fastapi import APIRouter, HTTPException
from app.schemas.document import ProcessRequest, ProcessResponse
from app.services.document_processor import DocumentProcessor

logger = logging.getLogger(__name__)
router = APIRouter()
processor = DocumentProcessor()


@router.post("/process", response_model=ProcessResponse)
async def process_document(request: ProcessRequest):
    try:
        result = await processor.process(
            document_id=request.documentId,
            knowledge_base_id=request.knowledgeBaseId,
            file_path=request.filePath,
            file_name=request.fileName,
            file_type=request.fileType
        )
        return ProcessResponse(
            success=result["success"],
            status=result["status"],
            chunkCount=result["chunkCount"],
            message=result["message"]
        )
    except Exception as e:
        logger.error("处理失败: %s", str(e))
        raise HTTPException(status_code=500, detail=str(e))
