import logging

from app.models.schemas import EntityItem, SpeechAnalyzeResponse, VadSegment
from app.services.asr_service import AsrService
from app.services.llm_service import LlmService
from app.services.se_service import SeService
from app.services.vad_service import VadService

logger = logging.getLogger(__name__)


class SpeechPipelineService:
    def __init__(self, vad_service: VadService, se_service: SeService, asr_service: AsrService, llm_service: LlmService):
        self._vad_service = vad_service
        self._se_service = se_service
        self._asr_service = asr_service
        self._llm_service = llm_service

    def analyze(self, task_id: int, original_file_path: str) -> SpeechAnalyzeResponse:
        logger.info('[pipeline] start taskId=%s', task_id)

        try:
            logger.info('[pipeline] stage=vad status=start provider=%s fallback=%s', self._vad_service.provider_name, self._vad_service.provider_meta.fallback_used)
            vad = self._vad_service.detect(original_file_path)
            logger.info('[pipeline] stage=vad status=success provider=%s segments=%s', self._vad_service.provider_name, len(vad.segments))
        except Exception as ex:
            logger.exception('[pipeline] stage=vad status=fail provider=%s err=%s', self._vad_service.provider_name, ex)
            raise RuntimeError(f'VAD stage failed: {ex}') from ex

        try:
            logger.info('[pipeline] stage=se status=start provider=%s fallback=%s', self._se_service.provider_name, self._se_service.provider_meta.fallback_used)
            se = self._se_service.enhance(task_id, original_file_path, vad.segments)
            logger.info('[pipeline] stage=se status=success provider=%s enhancedFilePath=%s', self._se_service.provider_name, se.enhanced_file_path)
        except Exception as ex:
            logger.exception('[pipeline] stage=se status=fail provider=%s err=%s', self._se_service.provider_name, ex)
            raise RuntimeError(f'SE stage failed: {ex}') from ex

        try:
            logger.info('[pipeline] stage=asr status=start provider=%s fallback=%s', self._asr_service.provider_name, self._asr_service.provider_meta.fallback_used)
            asr = self._asr_service.transcribe(task_id, se.enhanced_file_path, vad.segments)
            logger.info('[pipeline] stage=asr status=success provider=%s transcriptLen=%s', self._asr_service.provider_name, len(asr.transcript_text or ''))
        except Exception as ex:
            logger.exception('[pipeline] stage=asr status=fail provider=%s err=%s', self._asr_service.provider_name, ex)
            raise RuntimeError(f'ASR stage failed: {ex}') from ex

        try:
            logger.info('[pipeline] stage=llm status=start provider=%s fallback=%s', self._llm_service.provider_name, self._llm_service.provider_meta.fallback_used)
            llm = self._llm_service.analyze(task_id, asr.transcript_text)
            logger.info('[pipeline] stage=llm status=success provider=%s riskLevel=%s eventType=%s', self._llm_service.provider_name, llm.risk_level, llm.event_type)
        except Exception as ex:
            logger.exception('[pipeline] stage=llm status=fail provider=%s err=%s', self._llm_service.provider_name, ex)
            raise RuntimeError(f'LLM stage failed: {ex}') from ex

        provider_info = {
            'vad': self._vad_service.provider_name,
            'se': self._se_service.provider_name,
            'asr': self._asr_service.provider_name,
            'llm': self._llm_service.provider_name,
        }
        stage_info = {
            'vad': _meta_to_dict(self._vad_service.provider_meta),
            'se': _meta_to_dict(self._se_service.provider_meta),
            'asr': _meta_to_dict(self._asr_service.provider_meta),
            'llm': _meta_to_dict(self._llm_service.provider_meta),
        }
        fallback_info = {
            stage: {
                'fallbackUsed': info.get('fallbackUsed'),
                'fallbackReason': info.get('fallbackReason'),
            }
            for stage, info in stage_info.items()
        }

        logger.info('[pipeline] finish taskId=%s providerInfo=%s', task_id, provider_info)

        return SpeechAnalyzeResponse(
            success=True,
            taskId=task_id,
            enhancedFilePath=se.enhanced_file_path,
            transcriptText=asr.transcript_text,
            confidence=asr.confidence,
            riskLevel=llm.risk_level,
            eventType=llm.event_type,
            eventSummary=llm.event_summary,
            reason=llm.reason,
            entities=[
                EntityItem(entityType=e.entity_type, entityValue=e.entity_value, confidence=e.confidence)
                for e in llm.entities
            ],
            vadSegments=[VadSegment(startMs=s.start_ms, endMs=s.end_ms) for s in vad.segments],
            providers=provider_info,
            message='speech analyze success',
            rawLlmResponse=llm.raw_response,
            providerInfo=provider_info,
            stageInfo=stage_info,
            fallbackInfo=fallback_info,
            debug={
                'hasSpeech': vad.has_speech,
                'stageInfo': stage_info,
                'fallbackInfo': fallback_info,
            },
        )



def _meta_to_dict(meta):
    return {
        'requestedProvider': meta.requested_provider,
        'effectiveProvider': meta.effective_provider,
        'effectiveType': meta.effective_type,
        'fallbackUsed': meta.fallback_used,
        'fallbackReason': meta.fallback_reason,
    }
