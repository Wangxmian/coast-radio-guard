import logging
from functools import lru_cache
from pathlib import Path

from app.core.config import Settings, load_settings
from app.services.asr_service import AsrService
from app.services.llm_service import LlmService
from app.services.se_service import SeService
from app.services.speech_pipeline import SpeechPipelineService
from app.services.vad_service import VadService

logger = logging.getLogger(__name__)


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return load_settings()


@lru_cache(maxsize=1)
def get_vad_service() -> VadService:
    return VadService(get_settings())


@lru_cache(maxsize=1)
def get_se_service() -> SeService:
    return SeService(get_settings())


@lru_cache(maxsize=1)
def get_asr_service() -> AsrService:
    return AsrService(get_settings())


@lru_cache(maxsize=1)
def get_llm_service() -> LlmService:
    return LlmService(get_settings())


@lru_cache(maxsize=1)
def get_speech_pipeline_service() -> SpeechPipelineService:
    return SpeechPipelineService(
        vad_service=get_vad_service(),
        se_service=get_se_service(),
        asr_service=get_asr_service(),
        llm_service=get_llm_service(),
    )


def warmup_services() -> None:
    settings = get_settings()
    key_detected = bool(settings.siliconflow_api_key)
    key_source = settings.siliconflow_api_key_source
    masked_key_hint = "set" if key_detected else "empty"
    gtcrn_project_exists = bool(settings.gtcrn_project_dir and Path(settings.gtcrn_project_dir).expanduser().exists())
    gtcrn_ckpt_exists = bool(settings.gtcrn_checkpoint_dir and Path(settings.gtcrn_checkpoint_dir).expanduser().exists())
    logger.info(
        "provider startup summary: fallbackAllowed=%s, ai_use_real[vad=%s,se=%s,asr=%s,llm=%s]",
        settings.ai_allow_provider_fallback,
        settings.ai_use_real_vad,
        settings.ai_use_real_se,
        settings.ai_use_real_asr,
        settings.ai_use_real_llm,
    )
    logger.info(
        "llm key check: siliconflow_api_key=%s source=%s llm_provider=%s",
        masked_key_hint,
        key_source,
        settings.llm_provider,
    )
    logger.info(
        "gtcrn check: project_dir=%s exists=%s checkpoint_dir=%s exists=%s",
        settings.gtcrn_project_dir or "(empty)",
        gtcrn_project_exists,
        settings.gtcrn_checkpoint_dir or "(empty)",
        gtcrn_ckpt_exists,
    )

    vad = get_vad_service()
    se = get_se_service()
    asr = get_asr_service()
    llm = get_llm_service()

    for name, service in (("vad", vad), ("se", se), ("asr", asr), ("llm", llm)):
        meta = service.provider_meta
        logger.info(
            "provider[%s] requested=%s effective=%s type=%s fallback=%s reason=%s",
            name,
            meta.requested_provider,
            meta.effective_provider,
            meta.effective_type,
            meta.fallback_used,
            meta.fallback_reason,
        )
