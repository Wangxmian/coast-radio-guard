import logging

from app.services.providers.llm_common import (
    build_correction_prompt,
    build_llm_prompt,
    extract_openai_content,
    normalize_correction_content,
    parse_llm_json,
)
from app.services.types import LlmChatOutput, LlmCorrectionOutput, LlmEntityOutput, LlmOutput

logger = logging.getLogger(__name__)


class OpenAiCompatibleLlmProvider:
    provider_name = "openai-compatible"

    def __init__(self, provider_name: str, base_url: str, model: str, api_key: str, timeout_sec: float):
        try:
            import httpx  # type: ignore
        except Exception as ex:  # pragma: no cover
            raise RuntimeError(f"httpx is required for {provider_name}: {ex}") from ex

        self.provider_name = provider_name
        self._api_key = api_key or ""
        self._model = model
        self._base_url = base_url.rstrip("/")
        self._client = httpx.Client(timeout=timeout_sec)

    def analyze(self, task_id: int, transcript_text: str) -> LlmOutput:
        logger.info("LLM request start: provider=%s model=%s taskId=%s", self.provider_name, self._model, task_id)
        content = self._chat_completion(
            messages=[
                {
                    "role": "system",
                    "content": (
                        "You are a maritime coast radio risk analyst. "
                        "Return only valid JSON, no markdown, no explanation."
                    ),
                },
                {
                    "role": "user",
                    "content": build_llm_prompt(task_id=task_id, transcript_text=transcript_text),
                },
            ],
            temperature=0.2,
        )
        parsed = parse_llm_json(content)
        logger.info(
            "LLM parse success: provider=%s riskLevel=%s eventType=%s",
            self.provider_name,
            parsed.get("riskLevel"),
            parsed.get("eventType"),
        )

        return LlmOutput(
            risk_level=parsed["riskLevel"],
            event_type=parsed["eventType"],
            event_summary=parsed["eventSummary"],
            reason=parsed["reason"],
            entities=[
                LlmEntityOutput(
                    entity_type=e["entityType"],
                    entity_value=e["entityValue"],
                    confidence=e.get("confidence"),
                )
                for e in parsed["entities"]
            ],
            raw_response=content,
        )

    def chat(self, message: str) -> LlmChatOutput:
        content = self._chat_completion(
            messages=[
                {
                    "role": "system",
                    "content": (
                        "你是海岸电台智能离线值守系统的运维助手。"
                        "回答要简洁、专业，尽量给出可执行结论。"
                    ),
                },
                {"role": "user", "content": message},
            ],
            temperature=0.3,
        )
        return LlmChatOutput(answer=content.strip(), raw_response=content)

    def correct_transcript(self, task_id: int, transcript_text: str) -> LlmCorrectionOutput:
        logger.info("LLM correction start: provider=%s model=%s taskId=%s", self.provider_name, self._model, task_id)
        content = self._chat_completion(
            messages=[
                {
                    "role": "system",
                    "content": "你是一个中文 ASR 纠错助手，只能做最小必要修改，只返回纠错后的文本。",
                },
                {
                    "role": "user",
                    "content": build_correction_prompt(task_id=task_id, transcript_text=transcript_text),
                },
            ],
            temperature=0.0,
        )
        corrected = normalize_correction_content(content, transcript_text)
        logger.info("LLM correction success: provider=%s taskId=%s changed=%s", self.provider_name, task_id, corrected != transcript_text)
        return LlmCorrectionOutput(corrected_transcript=corrected, raw_response=content)

    def _chat_completion(self, messages: list[dict], temperature: float) -> str:
        payload = {
            "model": self._model,
            "messages": messages,
            "temperature": temperature,
        }
        headers = {"Content-Type": "application/json"}
        if self._api_key:
            headers["Authorization"] = f"Bearer {self._api_key}"

        response = self._client.post(
            f"{self._base_url}/chat/completions",
            headers=headers,
            json=payload,
        )
        logger.info("LLM request done: provider=%s status=%s", self.provider_name, response.status_code)
        if response.status_code >= 400:
            logger.error(
                "LLM request failed: provider=%s status=%s body=%s",
                self.provider_name,
                response.status_code,
                response.text[:500],
            )
        response.raise_for_status()
        data = response.json()
        return extract_openai_content(data)


class SiliconFlowLlmProvider(OpenAiCompatibleLlmProvider):
    provider_name = "siliconflow"

    def __init__(self, api_key: str, base_url: str, model: str, timeout_sec: float):
        if not api_key:
            raise ValueError("SILICONFLOW_API_KEY is empty")
        super().__init__(
            provider_name=self.provider_name,
            base_url=base_url,
            model=model,
            api_key=api_key,
            timeout_sec=timeout_sec,
        )
