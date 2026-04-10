from app.core.config import Settings
from app.services.provider_factory import ProviderMeta, select_se_provider
from app.services.types import SeOutput, VadSegmentType


class SeService:
    def __init__(self, settings: Settings):
        selection = select_se_provider(settings)
        self._provider = selection.provider
        self._meta = selection.meta

    @property
    def provider_name(self) -> str:
        return self._provider.provider_name

    @property
    def provider_meta(self) -> ProviderMeta:
        return self._meta

    def enhance(self, task_id: int, original_file_path: str, vad_segments: list[VadSegmentType] | None = None) -> SeOutput:
        return self._provider.enhance(task_id, original_file_path, vad_segments)
