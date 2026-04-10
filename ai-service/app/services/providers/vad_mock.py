from app.services.types import VadOutput, VadSegmentType


class MockVadProvider:
    provider_name = "mock-vad"

    def detect(self, audio_path: str) -> VadOutput:
        if not audio_path:
            return VadOutput(has_speech=False, segments=[])
        return VadOutput(
            has_speech=True,
            segments=[VadSegmentType(start_ms=0, end_ms=30000)],
        )
