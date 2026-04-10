from __future__ import annotations

import logging
import os
import tempfile
import uuid

from app.models.schemas import VadSegment
from app.services.asr_service import AsrService
from app.services.provider_factory import provider_meta_to_dict
from app.services.realtime_session_manager import RealtimeTranscriptSegment, StreamingSessionManager, utc_now_iso
from app.services.types import VadSegmentType
from app.services.vad_service import VadService

logger = logging.getLogger(__name__)


class RealtimeSpeechService:
    def __init__(self, session_manager: StreamingSessionManager, vad_service: VadService, asr_service: AsrService):
        self._session_manager = session_manager
        self._vad_service = vad_service
        self._asr_service = asr_service

    def start_session(self, session_id: str | None, channel_id: int | None, mode: str | None) -> dict:
        effective_id = (session_id or "").strip() or str(uuid.uuid4())
        session = self._session_manager.create(effective_id, channel_id, mode or "chunked")
        logger.info("realtime session started, sessionId=%s channelId=%s mode=%s", session.session_id, channel_id, session.mode)
        return self._session_payload(session)

    def stop_session(self, session_id: str) -> dict:
        session = self._session_manager.stop(session_id)
        if session is None:
            raise ValueError(f"unknown realtime session: {session_id}")
        logger.info("realtime session stopped, sessionId=%s chunks=%s", session.session_id, session.chunk_count)
        return self._session_payload(session)

    def get_status(self, session_id: str) -> dict:
        session = self._session_manager.get(session_id)
        if session is None:
            raise ValueError(f"unknown realtime session: {session_id}")
        return self._session_payload(session)

    def process_chunk(
        self,
        session_id: str,
        audio_name: str,
        payload: bytes,
        task_id: int | None,
        start_time: int | None,
        end_time: int | None,
    ) -> dict:
        session = self._session_manager.get(session_id)
        if session is None:
            raise ValueError(f"unknown realtime session: {session_id}")
        if session.status != "ACTIVE":
            raise ValueError(f"realtime session is not active: {session_id}")
        if not payload:
            raise ValueError("empty realtime audio chunk")

        suffix = os.path.splitext(audio_name or "chunk.wav")[1] or ".wav"
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix, prefix="crg_rt_") as tmp:
            tmp.write(payload)
            tmp_path = tmp.name

        try:
            vad_output = self._vad_service.detect(tmp_path)
            vad_meta = self._vad_service.provider_meta
            asr_meta = self._asr_service.provider_meta

            provider_info = {
                "vad": vad_meta.effective_provider,
                "asr": asr_meta.effective_provider,
            }
            stage_info = {
                "vad": provider_meta_to_dict(vad_meta),
                "asr": provider_meta_to_dict(asr_meta),
            }
            fallback_info = {
                "vad": {"fallbackUsed": vad_meta.fallback_used, "fallbackReason": vad_meta.fallback_reason},
                "asr": {"fallbackUsed": asr_meta.fallback_used, "fallbackReason": asr_meta.fallback_reason},
            }

            transcript_text = ""
            language = None
            if vad_output.has_speech:
                asr_output = self._asr_service.transcribe(task_id=task_id or 0, audio_path=tmp_path, vad_segments=vad_output.segments)
                transcript_text = asr_output.transcript_text
                language = asr_output.language
            else:
                fallback_info["asr"] = {"fallbackUsed": True, "fallbackReason": "vad:no_speech"}

            segment = RealtimeTranscriptSegment(
                segment_id=f"seg-{uuid.uuid4().hex[:12]}",
                transcript_text=transcript_text,
                start_time=start_time,
                end_time=end_time,
                is_final=True,
                has_speech=vad_output.has_speech,
                timestamp=utc_now_iso(),
                language=language,
            )
            session.append_segment(segment)
            session.last_provider_info = provider_info
            session.last_stage_info = stage_info
            session.last_fallback_info = fallback_info

            logger.info(
                "realtime chunk processed, sessionId=%s taskId=%s hasSpeech=%s transcriptLen=%s",
                session_id,
                task_id,
                vad_output.has_speech,
                len(transcript_text),
            )

            return {
                "success": True,
                "sessionId": session.session_id,
                "segmentId": segment.segment_id,
                "transcriptText": transcript_text,
                "accumulatedTranscript": session.transcript_text,
                "segments": [segment.to_dict()],
                "vadSegments": [_vad_segment_to_schema(item).model_dump() for item in vad_output.segments],
                "hasSpeech": vad_output.has_speech,
                "startTime": start_time,
                "endTime": end_time,
                "isFinal": True,
                "language": language,
                "message": "realtime chunk processed",
                "providerInfo": provider_info,
                "stageInfo": stage_info,
                "fallbackInfo": fallback_info,
                "timestamp": segment.timestamp,
            }
        finally:
            try:
                os.remove(tmp_path)
            except OSError:
                logger.warning("failed to cleanup realtime temp chunk file: %s", tmp_path)

    def _session_payload(self, session) -> dict:
        return {
            "success": True,
            "sessionId": session.session_id,
            "channelId": session.channel_id,
            "mode": session.mode,
            "status": session.status,
            "transcriptText": session.transcript_text,
            "segments": [item.to_dict() for item in session.segments],
            "chunkCount": session.chunk_count,
            "providerInfo": session.last_provider_info,
            "stageInfo": session.last_stage_info,
            "fallbackInfo": session.last_fallback_info,
            "timestamp": session.updated_at,
            "startedAt": session.started_at,
            "stoppedAt": session.stopped_at,
            "message": "realtime session ready",
            "capabilityNote": "最小可用版本：fsmn-vad 先筛语音，再用 SenseVoiceSmall 对短 chunk 做准实时识别",
        }


def _vad_segment_to_schema(item: VadSegmentType) -> VadSegment:
    return VadSegment(startMs=item.start_ms, endMs=item.end_ms)

