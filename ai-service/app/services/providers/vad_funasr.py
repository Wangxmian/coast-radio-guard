import logging
from typing import Any

from app.services.exceptions import ProviderUnavailableError
from app.services.providers.model_loader import resolve_model_source
from app.services.types import VadOutput, VadSegmentType

logger = logging.getLogger(__name__)


class FunAsrVadProvider:
    provider_name = "funasr-fsmn-vad"

    def __init__(
        self,
        model_name: str,
        model_dir: str = "",
        model_revision: str = "v2.0.4",
        max_end_silence_time: int | None = None,
        speech_noise_thres: float | None = None,
    ):
        try:
            from funasr import AutoModel  # type: ignore
        except Exception as ex:  # pragma: no cover
            raise ProviderUnavailableError(f"funasr import failed: {ex}") from ex

        self._model_source = resolve_model_source("vad", model_name=model_name, model_dir=model_dir)
        self._model_revision = model_revision
        self._max_end_silence_time = max_end_silence_time
        self._speech_noise_thres = speech_noise_thres

        try:
            self._model = AutoModel(model=self._model_source, model_revision=self._model_revision)
            logger.info(
                "FunASR VAD loaded, model=%s revision=%s max_end_silence_time=%s speech_noise_thres=%s",
                self._model_source,
                self._model_revision,
                self._max_end_silence_time,
                self._speech_noise_thres,
            )
        except Exception as ex:  # pragma: no cover
            raise ProviderUnavailableError(f"funasr vad init failed: {ex}") from ex

    def detect(self, audio_path: str) -> VadOutput:
        kwargs = {}
        if self._max_end_silence_time is not None:
            kwargs["max_end_silence_time"] = self._max_end_silence_time
        if self._speech_noise_thres is not None:
            kwargs["speech_noise_thres"] = self._speech_noise_thres

        try:
            result = self._model.generate(input=audio_path, **kwargs)
        except TypeError:
            # Some model wrappers do not expose these kwargs.
            result = self._model.generate(input=audio_path)

        segments = _parse_segments(result)
        return VadOutput(has_speech=bool(segments), segments=segments)



def _parse_segments(result: object) -> list[VadSegmentType]:
    if not isinstance(result, list) or not result:
        return []

    row = result[0] if isinstance(result[0], dict) else {}
    candidates = row.get("value") or row.get("timestamp") or row.get("segments") or []

    parsed: list[VadSegmentType] = []
    if isinstance(candidates, list):
        for item in candidates:
            if isinstance(item, (list, tuple)) and len(item) >= 2:
                start = _safe_int(item[0])
                end = _safe_int(item[1])
                if start is not None and end is not None and end >= start:
                    parsed.append(VadSegmentType(start_ms=start, end_ms=end))
            elif isinstance(item, dict):
                start = _safe_int(item.get("start") or item.get("start_ms") or item.get("startMs"))
                end = _safe_int(item.get("end") or item.get("end_ms") or item.get("endMs"))
                if start is not None and end is not None and end >= start:
                    parsed.append(VadSegmentType(start_ms=start, end_ms=end))

    return parsed



def _safe_int(value: object) -> int | None:
    try:
        return int(value)  # type: ignore[arg-type]
    except Exception:
        return None
