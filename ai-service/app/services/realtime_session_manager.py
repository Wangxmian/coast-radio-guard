from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
from threading import Lock
from typing import Any


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


@dataclass
class RealtimeTranscriptSegment:
    segment_id: str
    transcript_text: str
    start_time: int | None = None
    end_time: int | None = None
    is_final: bool = True
    has_speech: bool = True
    timestamp: str = field(default_factory=utc_now_iso)
    language: str | None = None

    def to_dict(self) -> dict[str, Any]:
        return {
            "segmentId": self.segment_id,
            "transcriptText": self.transcript_text,
            "startTime": self.start_time,
            "endTime": self.end_time,
            "isFinal": self.is_final,
            "hasSpeech": self.has_speech,
            "timestamp": self.timestamp,
            "language": self.language,
        }


@dataclass
class RealtimeSession:
    session_id: str
    channel_id: int | None
    mode: str
    status: str = "ACTIVE"
    transcript_text: str = ""
    segments: list[RealtimeTranscriptSegment] = field(default_factory=list)
    chunk_count: int = 0
    last_provider_info: dict[str, Any] = field(default_factory=dict)
    last_stage_info: dict[str, Any] = field(default_factory=dict)
    last_fallback_info: dict[str, Any] = field(default_factory=dict)
    started_at: str = field(default_factory=utc_now_iso)
    updated_at: str = field(default_factory=utc_now_iso)
    stopped_at: str | None = None

    def append_segment(self, segment: RealtimeTranscriptSegment, max_segments: int = 40) -> None:
        self.chunk_count += 1
        self.updated_at = utc_now_iso()
        self.segments.insert(0, segment)
        if len(self.segments) > max_segments:
            del self.segments[max_segments:]
        if segment.transcript_text:
            self.transcript_text = f"{self.transcript_text}\n{segment.transcript_text}".strip() if self.transcript_text else segment.transcript_text


class StreamingSessionManager:
    def __init__(self) -> None:
        self._lock = Lock()
        self._sessions: dict[str, RealtimeSession] = {}

    def create(self, session_id: str, channel_id: int | None, mode: str) -> RealtimeSession:
        session = RealtimeSession(session_id=session_id, channel_id=channel_id, mode=mode or "chunked")
        with self._lock:
            self._sessions[session_id] = session
        return session

    def get(self, session_id: str) -> RealtimeSession | None:
        with self._lock:
            return self._sessions.get(session_id)

    def stop(self, session_id: str) -> RealtimeSession | None:
        with self._lock:
            session = self._sessions.get(session_id)
            if session is None:
                return None
            session.status = "STOPPED"
            session.stopped_at = utc_now_iso()
            session.updated_at = session.stopped_at
            return session

