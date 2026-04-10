import logging
from pathlib import Path

from app.services.exceptions import ProviderUnavailableError
from app.services.types import AsrOutput, VadSegmentType

logger = logging.getLogger(__name__)


class FunAsrNanoProvider:
    provider_name = "funasr-nano"

    def __init__(
        self,
        model_dir: str,
        device: str = "cpu",
        language: str = "auto",
        use_itn: bool = True,
    ):
        try:
            from funasr import AutoModel  # type: ignore
        except Exception as ex:  # pragma: no cover
            raise ProviderUnavailableError(f"funasr import failed: {ex}") from ex

        resolved_model_dir = Path(model_dir).expanduser()
        if not resolved_model_dir.exists():
            raise ProviderUnavailableError(f"fun-asr-nano model dir not found: {resolved_model_dir}")
        if not (resolved_model_dir / "configuration.json").exists():
            raise ProviderUnavailableError(f"fun-asr-nano configuration.json missing: {resolved_model_dir}")
        if not (resolved_model_dir / "model.pt").exists():
            raise ProviderUnavailableError(f"fun-asr-nano model.pt missing: {resolved_model_dir}")

        self._model_dir = str(resolved_model_dir)
        self._device = device or "cpu"
        self._language = language or "auto"
        self._use_itn = use_itn

        try:
            self._model = AutoModel(
                model=self._model_dir,
                trust_remote_code=True,
                device=self._device,
                disable_update=True,
            )
            logger.info(
                "Fun-ASR-Nano loaded, modelDir=%s device=%s language=%s use_itn=%s",
                self._model_dir,
                self._device,
                self._language,
                self._use_itn,
            )
        except Exception as ex:  # pragma: no cover
            raise ProviderUnavailableError(
                "fun-asr-nano init failed: "
                + str(ex)
                + " (hint: install transformers/accelerate if tokenizer init fails)"
            ) from ex

    def transcribe(self, task_id: int, audio_path: str, vad_segments: list[VadSegmentType] | None = None) -> AsrOutput:
        _ = task_id
        _ = vad_segments

        kwargs = {
            "input": [audio_path],
            "cache": {},
            "batch_size": 1,
            "itn": self._use_itn,
        }
        if self._language and self._language.lower() != "auto":
            kwargs["language"] = _normalize_language(self._language)

        try:
            result = self._model.generate(**kwargs)
        except Exception as ex:
            raise RuntimeError(f"fun-asr-nano inference failed: {ex}") from ex

        if not isinstance(result, list) or not result:
            raise RuntimeError("empty asr result")

        row = result[0] if isinstance(result[0], dict) else {}
        raw_text = str(row.get("text") or row.get("value") or "").strip()
        if not raw_text:
            raise RuntimeError("asr text is empty")

        confidence = _safe_float(row.get("score") or row.get("confidence"))
        language = str(row.get("language") or row.get("lang") or self._language or "unknown")
        logger.info(
            "fun-asr-nano transcribe success, audio=%s device=%s language=%s textLen=%s",
            audio_path,
            self._device,
            language,
            len(raw_text),
        )
        return AsrOutput(transcript_text=raw_text, confidence=confidence, language=language)


def _normalize_language(value: str) -> str:
    mapping = {
        "zh": "中文",
        "cn": "中文",
        "chinese": "中文",
        "en": "英文",
        "english": "英文",
        "ja": "日文",
        "jp": "日文",
        "japanese": "日文",
    }
    key = value.strip().lower()
    return mapping.get(key, value)


def _safe_float(value: object) -> float | None:
    if value is None:
        return None
    try:
        return float(value)
    except Exception:
        return None
