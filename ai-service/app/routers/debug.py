from pathlib import Path

from fastapi import APIRouter

from app.models.schemas import DebugLlmCheckRequest, DebugLlmCheckResponse, DebugProvidersResponse
from app.services.provider_factory import provider_meta_to_dict
from app.services.service_factory import get_asr_service, get_llm_service, get_se_service, get_settings, get_vad_service

router = APIRouter()


@router.get('/providers', response_model=DebugProvidersResponse)
def debug_providers() -> DebugProvidersResponse:
    settings = get_settings()
    vad = get_vad_service().provider_meta
    se = get_se_service().provider_meta
    asr = get_asr_service().provider_meta
    llm = get_llm_service().provider_meta

    checkpoint_exists = False
    if settings.gtcrn_checkpoint_file and settings.gtcrn_checkpoint_dir:
        checkpoint_exists = (Path(settings.gtcrn_checkpoint_dir).expanduser() / settings.gtcrn_checkpoint_file).exists()
    elif settings.gtcrn_checkpoint_dir:
        checkpoint_exists = any(Path(settings.gtcrn_checkpoint_dir).expanduser().glob("*.tar"))

    return DebugProvidersResponse(
        success=True,
        fallbackAllowed=settings.ai_allow_provider_fallback,
        providers={
            'vad': {
                'configuredProvider': vad.requested_provider,
                'requestedProvider': vad.requested_provider,
                'effectiveProvider': vad.effective_provider,
                'effectiveType': vad.effective_type,
                'fallbackUsed': vad.fallback_used,
                'fallbackReason': vad.fallback_reason,
            },
            'se': {
                'configuredProvider': se.requested_provider,
                'requestedProvider': se.requested_provider,
                'effectiveProvider': se.effective_provider,
                'effectiveType': se.effective_type,
                'fallbackUsed': se.fallback_used,
                'fallbackReason': se.fallback_reason,
            },
            'asr': {
                'configuredProvider': asr.requested_provider,
                'requestedProvider': asr.requested_provider,
                'effectiveProvider': asr.effective_provider,
                'effectiveType': asr.effective_type,
                'fallbackUsed': asr.fallback_used,
                'fallbackReason': asr.fallback_reason,
            },
            'llm': {
                'configuredProvider': llm.requested_provider,
                'requestedProvider': llm.requested_provider,
                'effectiveProvider': llm.effective_provider,
                'effectiveType': llm.effective_type,
                'fallbackUsed': llm.fallback_used,
                'fallbackReason': llm.fallback_reason,
            },
            'correction': {
                'configuredProvider': llm.requested_provider,
                'requestedProvider': llm.requested_provider,
                'effectiveProvider': llm.effective_provider,
                'effectiveType': llm.effective_type,
                'fallbackUsed': llm.fallback_used,
                'fallbackReason': llm.fallback_reason,
            },
        },
        checks={
            "llmKeyDetected": bool(settings.siliconflow_api_key),
            "llmKeySource": settings.siliconflow_api_key_source,
            "gtcrnProjectDir": settings.gtcrn_project_dir or "",
            "gtcrnProjectDirExists": bool(settings.gtcrn_project_dir and Path(settings.gtcrn_project_dir).expanduser().exists()),
            "gtcrnCheckpointDir": settings.gtcrn_checkpoint_dir or "",
            "gtcrnCheckpointDirExists": bool(settings.gtcrn_checkpoint_dir and Path(settings.gtcrn_checkpoint_dir).expanduser().exists()),
            "gtcrnModelPath": settings.gtcrn_model_path or "",
            "gtcrnModelPathExists": bool(settings.gtcrn_model_path and Path(settings.gtcrn_model_path).expanduser().exists()),
            "se": {
                "configuredProvider": se.requested_provider,
                "effectiveProvider": se.effective_provider,
                "fallbackUsed": se.fallback_used,
                "projectDirExists": bool(settings.gtcrn_project_dir and Path(settings.gtcrn_project_dir).expanduser().exists()),
                "checkpointExists": bool(checkpoint_exists),
            },
        },
    )


@router.post('/llm-check', response_model=DebugLlmCheckResponse)
def debug_llm_check(req: DebugLlmCheckRequest) -> DebugLlmCheckResponse:
    service = get_llm_service()
    meta = service.provider_meta
    provider_info = {"llm": meta.effective_provider}
    stage_info = {"llm": provider_meta_to_dict(meta)}
    fallback_info = {"llm": {"fallbackUsed": meta.fallback_used, "fallbackReason": meta.fallback_reason}}
    try:
        output = service.analyze(task_id=req.taskId, transcript_text=req.transcriptText)
        return DebugLlmCheckResponse(
            success=True,
            providerInfo=provider_info,
            stageInfo=stage_info,
            fallbackInfo=fallback_info,
            riskLevel=output.risk_level,
            eventType=output.event_type,
            eventSummary=output.event_summary,
            reason=output.reason,
        )
    except Exception as ex:
        return DebugLlmCheckResponse(
            success=False,
            providerInfo=provider_info,
            stageInfo=stage_info,
            fallbackInfo=fallback_info,
            error=str(ex),
        )
