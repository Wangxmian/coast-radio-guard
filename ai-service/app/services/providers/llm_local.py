from app.services.providers.llm_siliconflow import OpenAiCompatibleLlmProvider


class LocalLlmProvider(OpenAiCompatibleLlmProvider):
    provider_name = "local-llm"

    def __init__(self, base_url: str, model: str, api_key: str, timeout_sec: float):
        if not base_url:
            raise ValueError("LOCAL_LLM_BASE_URL is empty")
        if not model:
            raise ValueError("LOCAL_LLM_MODEL is empty")
        super().__init__(
            provider_name=self.provider_name,
            base_url=base_url,
            model=model,
            api_key=api_key,
            timeout_sec=timeout_sec,
        )
