import logging

from fastapi import APIRouter

from app.models.schemas import SpeechAnalyzeRequest, SpeechAnalyzeResponse
from app.services.service_factory import get_speech_pipeline_service

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post('/analyze', response_model=SpeechAnalyzeResponse)
def analyze_speech(req: SpeechAnalyzeRequest) -> SpeechAnalyzeResponse:
    service = get_speech_pipeline_service()
    logger.info('Speech pipeline request received: taskId=%s', req.taskId)
    return service.analyze(task_id=req.taskId, original_file_path=req.originalFilePath)
