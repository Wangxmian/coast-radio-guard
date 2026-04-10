import logging

from fastapi import APIRouter, HTTPException

from app.models.schemas import AsrTranscribeRequest, AsrTranscribeResponse
from app.services.provider_factory import provider_meta_to_dict
from app.services.service_factory import get_asr_service

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post('/transcribe', response_model=AsrTranscribeResponse)
def transcribe_audio(req: AsrTranscribeRequest) -> AsrTranscribeResponse:
    service = get_asr_service()
    meta = service.provider_meta
    logger.info('ASR request received: taskId=%s, provider=%s, fallback=%s', req.taskId, service.provider_name, meta.fallback_used)

    try:
        output = service.transcribe(task_id=req.taskId, audio_path=req.enhancedFilePath)
    except Exception as ex:
        logger.exception("ASR transcribe failed: taskId=%s, path=%s", req.taskId, req.enhancedFilePath)
        raise HTTPException(status_code=400, detail=f"ASR transcribe failed: {ex}") from ex

    return AsrTranscribeResponse(
        success=True,
        transcriptText=output.transcript_text,
        confidence=output.confidence,
        language=output.language,
        message='asr success',
        providerInfo={'asr': meta.effective_provider},
        stageInfo={'asr': provider_meta_to_dict(meta)},
        fallbackInfo={'asr': {'fallbackUsed': meta.fallback_used, 'fallbackReason': meta.fallback_reason}},
    )
