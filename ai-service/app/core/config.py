import os
from dataclasses import dataclass

from dotenv import dotenv_values


def _as_bool(value: str | None, default: bool) -> bool:
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class Settings:
    ai_use_real_vad: bool
    ai_use_real_asr: bool
    ai_use_real_se: bool
    ai_use_real_llm: bool
    ai_allow_provider_fallback: bool

    vad_provider: str
    asr_provider: str
    se_provider: str
    llm_provider: str

    funasr_vad_model: str
    funasr_vad_model_dir: str
    funasr_vad_model_revision: str
    vad_max_end_silence_time: int
    vad_speech_noise_thres: float

    funasr_asr_model: str
    funasr_asr_model_dir: str
    funasr_asr_model_revision: str
    asr_language: str
    asr_use_itn: bool
    asr_batch_size_s: int
    asr_merge_vad: bool
    asr_merge_length_s: int
    asr_vad_max_single_segment_time: int

    gtcrn_model_path: str
    gtcrn_project_dir: str
    gtcrn_checkpoint_dir: str
    gtcrn_device: str
    gtcrn_output_dir: str
    gtcrn_checkpoint_file: str

    siliconflow_api_key: str
    siliconflow_api_key_source: str
    siliconflow_base_url: str
    siliconflow_model: str
    siliconflow_timeout_sec: float

    local_llm_base_url: str
    local_llm_model: str
    local_llm_api_key: str
    local_llm_timeout_sec: float

    enhanced_dir: str
    app_log_level: str


def _load_dotenv_map() -> dict[str, str]:
    env_file = os.getenv("AI_ENV_FILE", ".env")
    raw = dotenv_values(env_file)
    out: dict[str, str] = {}
    for k, v in raw.items():
        if k and v is not None:
            out[k] = str(v)
    return out


def _pick_env(name: str, dotenv_map: dict[str, str], default: str = "") -> tuple[str, str]:
    """
    Priority:
    1) process env (shell/export/systemd/docker env)
    2) .env file
    3) default
    """
    env_val = os.environ.get(name)
    if env_val is not None and env_val.strip() != "":
        return env_val.strip(), "process_env"

    dot_val = dotenv_map.get(name)
    if dot_val is not None and dot_val.strip() != "":
        return dot_val.strip(), "dotenv"

    return default, "default"


def _pick_env_or_empty(name: str, dotenv_map: dict[str, str]) -> tuple[str, str]:
    env_val = os.environ.get(name)
    if env_val is not None and env_val.strip() != "":
        return env_val.strip(), "process_env"

    dot_val = dotenv_map.get(name)
    if dot_val is not None and dot_val.strip() != "":
        return dot_val.strip(), "dotenv"

    return "", "missing"


def load_settings() -> Settings:
    dotenv_map = _load_dotenv_map()
    siliconflow_key, siliconflow_key_source = _pick_env_or_empty("SILICONFLOW_API_KEY", dotenv_map)

    return Settings(
        ai_use_real_vad=_as_bool(_pick_env("AI_USE_REAL_VAD", dotenv_map, "true")[0], True),
        ai_use_real_asr=_as_bool(_pick_env("AI_USE_REAL_ASR", dotenv_map, "true")[0], True),
        ai_use_real_se=_as_bool(_pick_env("AI_USE_REAL_SE", dotenv_map, "false")[0], False),
        ai_use_real_llm=_as_bool(_pick_env("AI_USE_REAL_LLM", dotenv_map, "true")[0], True),
        ai_allow_provider_fallback=_as_bool(_pick_env("AI_ALLOW_PROVIDER_FALLBACK", dotenv_map, "true")[0], True),
        vad_provider=_pick_env("VAD_PROVIDER", dotenv_map, "funasr")[0].strip().lower(),
        asr_provider=_pick_env("ASR_PROVIDER", dotenv_map, "funasr")[0].strip().lower(),
        se_provider=_pick_env("SE_PROVIDER", dotenv_map, "mock")[0].strip().lower(),
        llm_provider=_pick_env("LLM_PROVIDER", dotenv_map, "siliconflow")[0].strip().lower(),
        funasr_vad_model=_pick_env("FUNASR_VAD_MODEL", dotenv_map, "iic/speech_fsmn_vad_zh-cn-16k-common-pytorch")[0].strip(),
        funasr_vad_model_dir=_pick_env("FUNASR_VAD_MODEL_DIR", dotenv_map, "")[0].strip(),
        funasr_vad_model_revision=_pick_env("FUNASR_VAD_MODEL_REVISION", dotenv_map, "v2.0.4")[0].strip(),
        vad_max_end_silence_time=int(_pick_env("VAD_MAX_END_SILENCE_TIME", dotenv_map, "800")[0]),
        vad_speech_noise_thres=float(_pick_env("VAD_SPEECH_NOISE_THRES", dotenv_map, "0.6")[0]),
        funasr_asr_model=_pick_env("FUNASR_ASR_MODEL", dotenv_map, "iic/SenseVoiceSmall")[0].strip(),
        funasr_asr_model_dir=_pick_env("FUNASR_ASR_MODEL_DIR", dotenv_map, "")[0].strip(),
        funasr_asr_model_revision=_pick_env("FUNASR_ASR_MODEL_REVISION", dotenv_map, "master")[0].strip(),
        asr_language=_pick_env("ASR_LANGUAGE", dotenv_map, "auto")[0].strip(),
        asr_use_itn=_as_bool(_pick_env("ASR_USE_ITN", dotenv_map, "true")[0], True),
        asr_batch_size_s=int(_pick_env("ASR_BATCH_SIZE_S", dotenv_map, "60")[0]),
        asr_merge_vad=_as_bool(_pick_env("ASR_MERGE_VAD", dotenv_map, "false")[0], False),
        asr_merge_length_s=int(_pick_env("ASR_MERGE_LENGTH_S", dotenv_map, "15")[0]),
        asr_vad_max_single_segment_time=int(_pick_env("ASR_VAD_MAX_SINGLE_SEGMENT_TIME", dotenv_map, "30000")[0]),
        gtcrn_model_path=_pick_env("GTCRN_MODEL_PATH", dotenv_map, "")[0].strip(),
        gtcrn_project_dir=_pick_env("GTCRN_PROJECT_DIR", dotenv_map, "")[0].strip(),
        gtcrn_checkpoint_dir=_pick_env("GTCRN_CHECKPOINT_DIR", dotenv_map, "")[0].strip(),
        gtcrn_device=_pick_env("GTCRN_DEVICE", dotenv_map, "cpu")[0].strip(),
        gtcrn_output_dir=_pick_env("GTCRN_OUTPUT_DIR", dotenv_map, "./data/enhanced")[0].strip(),
        gtcrn_checkpoint_file=_pick_env("GTCRN_CHECKPOINT_FILE", dotenv_map, "")[0].strip(),
        siliconflow_api_key=siliconflow_key,
        siliconflow_api_key_source=siliconflow_key_source,
        siliconflow_base_url=_pick_env("SILICONFLOW_BASE_URL", dotenv_map, "https://api.siliconflow.cn/v1")[0].rstrip("/"),
        siliconflow_model=_pick_env("SILICONFLOW_MODEL", dotenv_map, "deepseek-ai/DeepSeek-V3.2")[0].strip(),
        siliconflow_timeout_sec=float(_pick_env("SILICONFLOW_TIMEOUT_SEC", dotenv_map, "30")[0]),
        local_llm_base_url=_pick_env("LOCAL_LLM_BASE_URL", dotenv_map, "")[0].rstrip("/"),
        local_llm_model=_pick_env("LOCAL_LLM_MODEL", dotenv_map, "")[0].strip(),
        local_llm_api_key=_pick_env("LOCAL_LLM_API_KEY", dotenv_map, "")[0].strip(),
        local_llm_timeout_sec=float(_pick_env("LOCAL_LLM_TIMEOUT_SEC", dotenv_map, "30")[0]),
        enhanced_dir=_pick_env("ENHANCED_DIR", dotenv_map, "/tmp/coast-radio-guard/enhanced")[0].strip(),
        app_log_level=_pick_env("APP_LOG_LEVEL", dotenv_map, "INFO")[0].strip().upper(),
    )
