import logging

from app.services.exceptions import ProviderUnavailableError
from app.services.providers.model_loader import resolve_model_source
from app.services.types import AsrOutput, VadSegmentType

logger = logging.getLogger(__name__)


class FunAsrSenseVoiceProvider:
    provider_name = "funasr-sensevoice-small"

    def __init__(
        self,
        model_name: str,
        model_dir: str = "",
        model_revision: str = "master",
        language: str = "auto",
        use_itn: bool = True,
        batch_size_s: int = 60,
        merge_vad: bool = False,
        merge_length_s: int = 15,
        vad_max_single_segment_time: int = 30000,
    ):
        try:
            from funasr import AutoModel  # type: ignore
        except Exception as ex:  # pragma: no cover
            raise ProviderUnavailableError(f"funasr import failed: {ex}") from ex

        self._model_source = resolve_model_source("asr", model_name=model_name, model_dir=model_dir)
        self._model_revision = model_revision
        self._language = language
        self._use_itn = use_itn
        self._batch_size_s = batch_size_s
        self._merge_vad = merge_vad
        self._merge_length_s = merge_length_s
        self._vad_kwargs = {"max_single_segment_time": vad_max_single_segment_time}

        try:
            self._model = AutoModel(
                model=self._model_source,
                model_revision=self._model_revision,
                trust_remote_code=True,
            )
            logger.info(
                "FunASR ASR loaded, model=%s revision=%s language=%s use_itn=%s batch_size_s=%s merge_vad=%s merge_length_s=%s",
                self._model_source,
                self._model_revision,
                self._language,
                self._use_itn,
                self._batch_size_s,
                self._merge_vad,
                self._merge_length_s,
            )
        except Exception as ex:  # pragma: no cover
            raise ProviderUnavailableError(f"funasr asr init failed: {ex}") from ex

        self._postprocess = None
        try:
            from funasr.utils.postprocess_utils import rich_transcription_postprocess  # type: ignore

            self._postprocess = rich_transcription_postprocess
        except Exception:
            logger.warning("rich_transcription_postprocess not available, raw text will be used")

    def transcribe(self, task_id: int, audio_path: str, vad_segments: list[VadSegmentType] | None = None) -> AsrOutput:
        _ = task_id
        _ = vad_segments

        # Prefer in-memory PCM input to avoid ffmpeg dependency on local runtime.
        model_input = audio_path
        model_fs = None
        try:
            import numpy as np  # type: ignore
            import soundfile as sf  # type: ignore

            waveform, sr = sf.read(audio_path, dtype="float32")
            if isinstance(waveform, np.ndarray) and waveform.ndim > 1:
                waveform = waveform.mean(axis=1)
            model_input = waveform
            model_fs = int(sr)
            logger.debug("ASR using soundfile-loaded waveform, sample_rate=%s", model_fs)
        except Exception as ex:
            logger.warning("ASR waveform preload failed, fallback to file path input: %s", ex)

        # We keep external VAD in pipeline; ASR focuses on recognition.
        kwargs = dict(
            input=model_input,
            cache={},
            language=self._language,
            use_itn=self._use_itn,
            batch_size_s=self._batch_size_s,
            merge_vad=self._merge_vad,
            merge_length_s=self._merge_length_s,
            vad_kwargs=self._vad_kwargs,
        )
        if model_fs is not None:
            kwargs["fs"] = model_fs

        try:
            result = self._model.generate(**kwargs)
        except Exception as ex:
            raise RuntimeError(
                "ASR inference failed. If you are using file-path input, install ffmpeg "
                "or keep audio as wav readable by soundfile. detail: " + str(ex)
            ) from ex

        if not isinstance(result, list) or not result:
            raise RuntimeError("empty asr result")

        row = result[0] if isinstance(result[0], dict) else {}
        raw_text = str(row.get("text") or row.get("value") or "").strip()
        if not raw_text:
            raise RuntimeError("asr text is empty")

        text = self._postprocess(raw_text) if self._postprocess else raw_text
        confidence = _safe_float(row.get("score") or row.get("confidence"))
        language = str(row.get("language") or row.get("lang") or self._language)
        return AsrOutput(transcript_text=text, confidence=confidence, language=language)



def _safe_float(value: object) -> float | None:
    if value is None:
        return None
    try:
        return float(value)
    except Exception:
        return None
