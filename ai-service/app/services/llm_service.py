import json

from app.core.config import Settings
from app.services.provider_factory import ProviderMeta, select_llm_provider
from app.services.types import LlmChatOutput, LlmCorrectionOutput, LlmOutput


class LlmService:
    def __init__(self, settings: Settings):
        selection = select_llm_provider(settings)
        self._provider = selection.provider
        self._meta = selection.meta

    @property
    def provider_name(self) -> str:
        return self._provider.provider_name

    @property
    def provider_meta(self) -> ProviderMeta:
        return self._meta

    def analyze(self, task_id: int, transcript_text: str) -> LlmOutput:
        return self._provider.analyze(task_id, transcript_text)

    def correct_transcript(self, task_id: int, transcript_text: str) -> LlmCorrectionOutput:
        if hasattr(self._provider, "correct_transcript"):
            return self._provider.correct_transcript(task_id, transcript_text)  # type: ignore[no-any-return]
        return LlmCorrectionOutput(corrected_transcript=transcript_text, raw_response=transcript_text)

    def chat(self, message: str) -> LlmChatOutput:
        if hasattr(self._provider, "chat"):
            return self._provider.chat(message)  # type: ignore[no-any-return]
        # fallback: reuse analyze shape and summarize.
        analyzed = self._provider.analyze(0, message)
        answer = f"风险等级：{analyzed.risk_level}，事件类型：{analyzed.event_type}。{analyzed.event_summary}"
        return LlmChatOutput(answer=answer, raw_response=analyzed.raw_response)

    def format_answer(self, intent: str, question: str, structured_data: object | None, fallback_answer: str | None) -> LlmChatOutput:
        if self._meta.effective_type == "mock":
            return LlmChatOutput(answer=(fallback_answer or "").strip(), raw_response=fallback_answer)

        formatted_data = json.dumps(structured_data, ensure_ascii=False, default=str, indent=2)
        prompt = (
            "请你将下面这份值守平台结构化查询结果，整理成更自然、更清晰、更适合后台值守场景的中文回答。\n"
            "要求：\n"
            "1. 不要编造结构化数据里没有的信息。\n"
            "2. 保留关键数字、时间、状态、频道名称。\n"
            "3. 先给总结，再给必要明细。\n"
            "4. 如果数据为空或不足，请明确说明。\n"
            "5. 不要输出 JSON，不要泄露数据库字段名。\n\n"
            f"问题意图：{intent}\n"
            f"用户问题：{question}\n"
            f"结构化数据：\n{formatted_data}\n\n"
            f"后端兜底回答：\n{fallback_answer or ''}\n"
        )
        if hasattr(self._provider, "chat"):
            return self._provider.chat(prompt)  # type: ignore[no-any-return]
        return LlmChatOutput(answer=(fallback_answer or "").strip(), raw_response=fallback_answer)
