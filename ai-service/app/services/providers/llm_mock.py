from app.services.types import LlmChatOutput, LlmCorrectionOutput, LlmEntityOutput, LlmOutput


class MockLlmProvider:
    provider_name = "mock-llm"

    def analyze(self, task_id: int, transcript_text: str) -> LlmOutput:
        _ = task_id
        text = transcript_text.lower()

        if "mayday" in text or "fire" in text:
            return LlmOutput(
                risk_level="HIGH",
                event_type="FIRE_DISTRESS",
                event_summary="船舶报告机舱起火并请求立即救援。",
                reason="文本命中 mayday/fire/immediate assistance 等遇险语义。",
                entities=[
                    LlmEntityOutput(entity_type="INCIDENT_TYPE", entity_value="engine room fire", confidence=0.94),
                    LlmEntityOutput(entity_type="RESCUE_NEED", entity_value="immediate assistance", confidence=0.93),
                ],
                raw_response="",
            )

        if "collision" in text:
            return LlmOutput(
                risk_level="HIGH",
                event_type="COLLISION_RISK",
                event_summary="通信提示存在碰撞风险。",
                reason="文本命中 collision risk 关键语义。",
                entities=[
                    LlmEntityOutput(entity_type="INCIDENT_TYPE", entity_value="collision risk", confidence=0.92),
                ],
                raw_response="",
            )

        if "man overboard" in text:
            return LlmOutput(
                risk_level="HIGH",
                event_type="MAN_OVERBOARD",
                event_summary="通信报告人员落水，属于紧急事件。",
                reason="文本命中 man overboard。",
                entities=[
                    LlmEntityOutput(entity_type="INCIDENT_TYPE", entity_value="man overboard", confidence=0.95),
                ],
                raw_response="",
            )

        return LlmOutput(
            risk_level="LOW",
            event_type="ORDINARY_COMMUNICATION",
            event_summary="常规通信，未识别到明显风险。",
            reason="未命中高风险关键词。",
            entities=[],
            raw_response="",
        )

    def chat(self, message: str) -> LlmChatOutput:
        text = (message or "").strip()
        if not text:
            return LlmChatOutput(answer="请提供你要查询的问题。", raw_response="")
        lower = text.lower()
        if "告警" in text or "alarm" in lower:
            return LlmChatOutput(answer="当前为 mock 智能体：可通过“查今日告警/高风险事件/生成日报”快捷能力获取真实统计。", raw_response="")
        if "高风险" in text or "high risk" in lower:
            return LlmChatOutput(answer="当前为 mock 智能体：建议先查询告警中心 HIGH 级记录并结合任务分析详情复核。", raw_response="")
        return LlmChatOutput(answer=f"收到：{text}\n当前为 mock LLM 回答。", raw_response=text)

    def correct_transcript(self, task_id: int, transcript_text: str) -> LlmCorrectionOutput:
        _ = task_id
        return LlmCorrectionOutput(corrected_transcript=transcript_text, raw_response=transcript_text)
