import logging
import os
import tempfile
from datetime import datetime

from fastapi import APIRouter, File, Form, UploadFile

from app.models.schemas import RealtimeTranscribeResponse
from app.services.provider_factory import provider_meta_to_dict
from app.services.service_factory import get_asr_service

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post('/transcribe', response_model=RealtimeTranscribeResponse)
async def realtime_transcribe(
    audio: UploadFile = File(...),
    taskId: int | None = Form(default=None),
    startTime: int | None = Form(default=None),
    endTime: int | None = Form(default=None),
) -> RealtimeTranscribeResponse:
    service = get_asr_service()
    meta = service.provider_meta

    suffix = os.path.splitext(audio.filename or 'chunk.wav')[1] or '.wav'
    payload = await audio.read()
    if not payload:
        raise ValueError('empty realtime audio chunk')

    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix, prefix='crg_rt_') as tmp:
        tmp.write(payload)
        tmp_path = tmp.name

    logger.info(
        'Realtime ASR request: taskId=%s provider=%s fallback=%s bytes=%s',
        taskId,
        service.provider_name,
        meta.fallback_used,
        len(payload),
    )

    try:
        output = service.transcribe(task_id=taskId or 0, audio_path=tmp_path)
    finally:
        try:
            os.remove(tmp_path)
        except OSError:
            logger.warning('failed to cleanup temp chunk file: %s', tmp_path)

    timestamp = datetime.utcnow().isoformat() + 'Z'
    segment_id = f'seg-{int(datetime.utcnow().timestamp() * 1000)}'

    return RealtimeTranscribeResponse(
        success=True,
        segmentId=segment_id,
        transcriptText=output.transcript_text,
        startTime=startTime,
        endTime=endTime,
        isFinal=True,
        language=output.language,
        message='realtime asr success',
        providerInfo={'asr': meta.effective_provider},
        stageInfo={'asr': provider_meta_to_dict(meta)},
        fallbackInfo={'asr': {'fallbackUsed': meta.fallback_used, 'fallbackReason': meta.fallback_reason}},
        timestamp=timestamp,
    )
