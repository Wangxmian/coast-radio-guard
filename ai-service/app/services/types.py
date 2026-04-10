from dataclasses import dataclass, field


@dataclass
class VadSegmentType:
    start_ms: int
    end_ms: int


@dataclass
class VadOutput:
    has_speech: bool
    segments: list[VadSegmentType] = field(default_factory=list)


@dataclass
class SeOutput:
    enhanced_file_path: str


@dataclass
class AsrOutput:
    transcript_text: str
    confidence: float | None = None
    language: str | None = None


@dataclass
class LlmEntityOutput:
    entity_type: str
    entity_value: str
    confidence: float | None = None


@dataclass
class LlmOutput:
    risk_level: str
    event_type: str
    event_summary: str
    reason: str
    entities: list[LlmEntityOutput] = field(default_factory=list)
    raw_response: str | None = None


@dataclass
class LlmChatOutput:
    answer: str
    raw_response: str | None = None


@dataclass
class LlmCorrectionOutput:
    corrected_transcript: str
    raw_response: str | None = None
