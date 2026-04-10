from app.services.types import AsrOutput, VadSegmentType


class MockAsrProvider:
    provider_name = "mock-asr"

    _SCENARIOS = {
        0: (
            "Mayday mayday, this is vessel HaiXing 07, fire in engine room, need immediate assistance.",
            0.96,
        ),
        1: (
            "Coast station, we have possible collision risk with unidentified vessel at bearing zero nine zero.",
            0.93,
        ),
        2: (
            "Man overboard on starboard side, request rescue support immediately.",
            0.95,
        ),
        3: (
            "Routine traffic update, weather is clear and navigation remains normal.",
            0.92,
        ),
    }

    def transcribe(self, task_id: int, audio_path: str, vad_segments: list[VadSegmentType] | None = None) -> AsrOutput:
        _ = audio_path
        _ = vad_segments
        text, confidence = self._SCENARIOS[task_id % 4]
        return AsrOutput(transcript_text=text, confidence=confidence)
