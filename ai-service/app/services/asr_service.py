from app.core.config import Settings
from app.services.provider_factory import ProviderMeta, select_asr_provider
from app.services.types import AsrOutput, VadSegmentType


class AsrService:
    def __init__(self, settings: Settings):
        selection = select_asr_provider(settings)
        self._provider = selection.provider
        self._meta = selection.meta

    @property
    def provider_name(self) -> str:
        return self._provider.provider_name

    @property
    def provider_meta(self) -> ProviderMeta:
        return self._meta

    def transcribe(self, task_id: int, audio_path: str, vad_segments: list[VadSegmentType] | None = None) -> AsrOutput:
        return self._provider.transcribe(task_id, audio_path, vad_segments)
