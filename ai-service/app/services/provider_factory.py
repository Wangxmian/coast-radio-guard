import logging
from dataclasses import dataclass
from typing import Any, Callable

from app.core.config import Settings
from app.services.providers.asr_funasr import FunAsrSenseVoiceProvider
from app.services.providers.asr_mock import MockAsrProvider
from app.services.providers.llm_local import LocalLlmProvider
from app.services.providers.llm_mock import MockLlmProvider
from app.services.providers.llm_siliconflow import SiliconFlowLlmProvider
from app.services.providers.se_gtcrn import GtcrnSeProvider
from app.services.providers.se_mock import MockSeProvider
from app.services.providers.vad_funasr import FunAsrVadProvider
from app.services.providers.vad_mock import MockVadProvider

logger = logging.getLogger(__name__)


@dataclass
class ProviderMeta:
    stage: str
    requested_provider: str
    effective_provider: str
    effective_type: str
    fallback_used: bool
    fallback_reason: str | None = None


@dataclass
class ProviderSelection:
    provider: Any
    meta: ProviderMeta



def provider_meta_to_dict(meta: ProviderMeta) -> dict[str, Any]:
    return {
        "stage": meta.stage,
        "requestedProvider": meta.requested_provider,
        "effectiveProvider": meta.effective_provider,
        "effectiveType": meta.effective_type,
        "fallbackUsed": meta.fallback_used,
        "fallbackReason": meta.fallback_reason,
    }



def select_vad_provider(settings: Settings) -> ProviderSelection:
    requested = settings.vad_provider
    if not settings.ai_use_real_vad:
        provider = MockVadProvider()
        return ProviderSelection(
            provider=provider,
            meta=ProviderMeta("vad", requested, provider.provider_name, _provider_type(provider.provider_name), True, "AI_USE_REAL_VAD=false"),
        )

    if requested == "funasr":
        return _build_with_fallback(
            stage="vad",
            requested_provider=requested,
            primary_builder=lambda: FunAsrVadProvider(
                model_name=settings.funasr_vad_model,
                model_dir=settings.funasr_vad_model_dir,
                model_revision=settings.funasr_vad_model_revision,
                max_end_silence_time=settings.vad_max_end_silence_time,
                speech_noise_thres=settings.vad_speech_noise_thres,
            ),
            fallback_builder=lambda: MockVadProvider(),
            allow_fallback=settings.ai_allow_provider_fallback,
        )

    return _unknown_provider(stage="vad", requested_provider=requested, fallback_builder=lambda: MockVadProvider(), allow_fallback=settings.ai_allow_provider_fallback)



def select_asr_provider(settings: Settings) -> ProviderSelection:
    requested = settings.asr_provider
    if not settings.ai_use_real_asr:
        provider = MockAsrProvider()
        return ProviderSelection(
            provider=provider,
            meta=ProviderMeta("asr", requested, provider.provider_name, _provider_type(provider.provider_name), True, "AI_USE_REAL_ASR=false"),
        )

    if requested in {"funasr", "sensevoice"}:
        return _build_with_fallback(
            stage="asr",
            requested_provider=requested,
            primary_builder=lambda: FunAsrSenseVoiceProvider(
                model_name=settings.funasr_asr_model,
                model_dir=settings.funasr_asr_model_dir,
                model_revision=settings.funasr_asr_model_revision,
                language=settings.asr_language,
                use_itn=settings.asr_use_itn,
                batch_size_s=settings.asr_batch_size_s,
                merge_vad=settings.asr_merge_vad,
                merge_length_s=settings.asr_merge_length_s,
                vad_max_single_segment_time=settings.asr_vad_max_single_segment_time,
            ),
            fallback_builder=lambda: MockAsrProvider(),
            allow_fallback=settings.ai_allow_provider_fallback,
        )

    return _unknown_provider(stage="asr", requested_provider=requested, fallback_builder=lambda: MockAsrProvider(), allow_fallback=settings.ai_allow_provider_fallback)



def select_se_provider(settings: Settings) -> ProviderSelection:
    requested = settings.se_provider
    if not settings.ai_use_real_se:
        provider = MockSeProvider(settings.enhanced_dir)
        return ProviderSelection(
            provider=provider,
            meta=ProviderMeta("se", requested, provider.provider_name, _provider_type(provider.provider_name), True, "AI_USE_REAL_SE=false"),
        )

    if requested == "gtcrn":
        return _build_with_fallback(
            stage="se",
            requested_provider=requested,
            primary_builder=lambda: GtcrnSeProvider(
                model_path=settings.gtcrn_model_path,
                project_dir=settings.gtcrn_project_dir,
                checkpoint_dir=settings.gtcrn_checkpoint_dir,
                checkpoint_file=settings.gtcrn_checkpoint_file,
                device=settings.gtcrn_device,
                output_dir=settings.gtcrn_output_dir,
            ),
            fallback_builder=lambda: MockSeProvider(settings.enhanced_dir),
            allow_fallback=settings.ai_allow_provider_fallback,
        )

    if requested == "mock":
        provider = MockSeProvider(settings.enhanced_dir)
        return ProviderSelection(
            provider=provider,
            meta=ProviderMeta("se", requested, provider.provider_name, _provider_type(provider.provider_name), False, None),
        )

    return _unknown_provider(stage="se", requested_provider=requested, fallback_builder=lambda: MockSeProvider(settings.enhanced_dir), allow_fallback=settings.ai_allow_provider_fallback)



def select_llm_provider(settings: Settings) -> ProviderSelection:
    requested = settings.llm_provider
    if not settings.ai_use_real_llm:
        provider = MockLlmProvider()
        return ProviderSelection(
            provider=provider,
            meta=ProviderMeta("llm", requested, provider.provider_name, _provider_type(provider.provider_name), True, "AI_USE_REAL_LLM=false"),
        )

    if requested == "siliconflow":
        return _build_with_fallback(
            stage="llm",
            requested_provider=requested,
            primary_builder=lambda: SiliconFlowLlmProvider(
                api_key=settings.siliconflow_api_key,
                base_url=settings.siliconflow_base_url,
                model=settings.siliconflow_model,
                timeout_sec=settings.siliconflow_timeout_sec,
            ),
            fallback_builder=lambda: MockLlmProvider(),
            allow_fallback=settings.ai_allow_provider_fallback,
        )

    if requested == "local":
        return _build_with_fallback(
            stage="llm",
            requested_provider=requested,
            primary_builder=lambda: LocalLlmProvider(
                base_url=settings.local_llm_base_url,
                model=settings.local_llm_model,
                api_key=settings.local_llm_api_key,
                timeout_sec=settings.local_llm_timeout_sec,
            ),
            fallback_builder=lambda: MockLlmProvider(),
            allow_fallback=settings.ai_allow_provider_fallback,
        )

    if requested == "mock":
        provider = MockLlmProvider()
        return ProviderSelection(
            provider=provider,
            meta=ProviderMeta("llm", requested, provider.provider_name, _provider_type(provider.provider_name), False, None),
        )

    return _unknown_provider(stage="llm", requested_provider=requested, fallback_builder=lambda: MockLlmProvider(), allow_fallback=settings.ai_allow_provider_fallback)



def _build_with_fallback(
    stage: str,
    requested_provider: str,
    primary_builder: Callable[[], Any],
    fallback_builder: Callable[[], Any],
    allow_fallback: bool,
) -> ProviderSelection:
    try:
        provider = primary_builder()
        logger.info("%s provider selected: requested=%s effective=%s", stage, requested_provider, provider.provider_name)
        return ProviderSelection(
            provider=provider,
            meta=ProviderMeta(stage, requested_provider, provider.provider_name, _provider_type(provider.provider_name), False, None),
        )
    except Exception as ex:
        logger.warning("%s provider init failed: requested=%s, err=%s", stage, requested_provider, ex)
        if not allow_fallback:
            raise
        fallback = fallback_builder()
        logger.warning("%s provider fallback: requested=%s -> effective=%s", stage, requested_provider, fallback.provider_name)
        return ProviderSelection(
            provider=fallback,
            meta=ProviderMeta(stage, requested_provider, fallback.provider_name, _provider_type(fallback.provider_name), True, str(ex)),
        )



def _unknown_provider(stage: str, requested_provider: str, fallback_builder: Callable[[], Any], allow_fallback: bool) -> ProviderSelection:
    message = f"unknown provider '{requested_provider}' for stage '{stage}'"
    logger.warning(message)
    if not allow_fallback:
        raise RuntimeError(message)
    fallback = fallback_builder()
    return ProviderSelection(
        provider=fallback,
        meta=ProviderMeta(stage, requested_provider, fallback.provider_name, _provider_type(fallback.provider_name), True, message),
    )



def _provider_type(provider_name: str) -> str:
    return "mock" if "mock" in provider_name else "real"
