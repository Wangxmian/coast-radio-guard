from app.core.config import Settings
from app.services.provider_factory import ProviderMeta, select_vad_provider
from app.services.types import VadOutput


class VadService:
    def __init__(self, settings: Settings):
        selection = select_vad_provider(settings)
        self._provider = selection.provider
        self._meta = selection.meta

    @property
    def provider_name(self) -> str:
        return self._provider.provider_name

    @property
    def provider_meta(self) -> ProviderMeta:
        return self._meta

    def detect(self, audio_path: str) -> VadOutput:
        return self._provider.detect(audio_path)
