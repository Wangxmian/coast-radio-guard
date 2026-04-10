import logging

from fastapi import APIRouter

from app.models.schemas import SeEnhanceRequest, SeEnhanceResponse
from app.services.provider_factory import provider_meta_to_dict
from app.services.service_factory import get_se_service

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post('/enhance', response_model=SeEnhanceResponse)
def enhance_audio(req: SeEnhanceRequest) -> SeEnhanceResponse:
    service = get_se_service()
    meta = service.provider_meta
    logger.info('SE request received: taskId=%s, provider=%s, fallback=%s', req.taskId, service.provider_name, meta.fallback_used)

    output = service.enhance(task_id=req.taskId, original_file_path=req.originalFilePath)
    return SeEnhanceResponse(
        success=True,
        enhancedFilePath=output.enhanced_file_path,
        message='se success',
        providerInfo={'se': meta.effective_provider},
        stageInfo={'se': provider_meta_to_dict(meta)},
        fallbackInfo={'se': {'fallbackUsed': meta.fallback_used, 'fallbackReason': meta.fallback_reason}},
    )
