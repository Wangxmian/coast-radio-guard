import logging

from fastapi import APIRouter

from app.models.schemas import EntityItem, LlmAnalyzeRequest, LlmAnalyzeResponse, LlmChatRequest, LlmChatResponse, LlmCorrectionRequest, LlmCorrectionResponse, LlmFormatAnswerRequest
from app.services.provider_factory import provider_meta_to_dict
from app.services.service_factory import get_llm_service

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post('/analyze', response_model=LlmAnalyzeResponse)
def analyze(req: LlmAnalyzeRequest) -> LlmAnalyzeResponse:
    service = get_llm_service()
    meta = service.provider_meta
    logger.info('LLM request received: taskId=%s, provider=%s, fallback=%s', req.taskId, service.provider_name, meta.fallback_used)

    output = service.analyze(task_id=req.taskId, transcript_text=req.transcriptText)
    return LlmAnalyzeResponse(
        success=True,
        riskLevel=output.risk_level,
        eventType=output.event_type,
        eventSummary=output.event_summary,
        reason=output.reason,
        entities=[
            EntityItem(entityType=e.entity_type, entityValue=e.entity_value, confidence=e.confidence)
            for e in output.entities
        ],
        message='llm success',
        rawResponse=output.raw_response,
        providerInfo={'llm': meta.effective_provider},
        stageInfo={'llm': provider_meta_to_dict(meta)},
        fallbackInfo={'llm': {'fallbackUsed': meta.fallback_used, 'fallbackReason': meta.fallback_reason}},
    )


@router.post('/chat', response_model=LlmChatResponse)
def chat(req: LlmChatRequest) -> LlmChatResponse:
    service = get_llm_service()
    meta = service.provider_meta
    logger.info('LLM chat request received: provider=%s, fallback=%s', service.provider_name, meta.fallback_used)
    output = service.chat(req.message)
    return LlmChatResponse(
        success=True,
        answer=output.answer,
        message='llm chat success',
        rawResponse=output.raw_response,
        providerInfo={'llm': meta.effective_provider},
        stageInfo={'llm': provider_meta_to_dict(meta)},
        fallbackInfo={'llm': {'fallbackUsed': meta.fallback_used, 'fallbackReason': meta.fallback_reason}},
    )


@router.post('/format-answer', response_model=LlmChatResponse)
def format_answer(req: LlmFormatAnswerRequest) -> LlmChatResponse:
    service = get_llm_service()
    meta = service.provider_meta
    logger.info('LLM answer formatting request received: intent=%s provider=%s fallback=%s', req.intent, service.provider_name, meta.fallback_used)
    output = service.format_answer(
        intent=req.intent,
        question=req.question,
        structured_data=req.structuredData,
        fallback_answer=req.fallbackAnswer,
    )
    return LlmChatResponse(
        success=True,
        answer=output.answer,
        message='llm format answer success',
        rawResponse=output.raw_response,
        providerInfo={'llm': meta.effective_provider},
        stageInfo={'llm': provider_meta_to_dict(meta)},
        fallbackInfo={'llm': {'fallbackUsed': meta.fallback_used, 'fallbackReason': meta.fallback_reason}},
    )


@router.post('/correct', response_model=LlmCorrectionResponse)
def correct(req: LlmCorrectionRequest) -> LlmCorrectionResponse:
    service = get_llm_service()
    meta = service.provider_meta
    logger.info('LLM correction request received: taskId=%s, provider=%s, fallback=%s', req.taskId, service.provider_name, meta.fallback_used)
    output = service.correct_transcript(task_id=req.taskId, transcript_text=req.transcriptText)
    return LlmCorrectionResponse(
        success=True,
        correctedTranscript=output.corrected_transcript,
        message='llm correction success',
        rawResponse=output.raw_response,
        providerInfo={'correction': meta.effective_provider},
        stageInfo={'correction': provider_meta_to_dict(meta)},
        fallbackInfo={'correction': {'fallbackUsed': meta.fallback_used, 'fallbackReason': meta.fallback_reason}},
    )
