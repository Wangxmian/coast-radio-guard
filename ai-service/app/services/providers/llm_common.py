import json
import logging
from typing import Any

logger = logging.getLogger(__name__)

_ALLOWED_RISK_LEVELS = {"LOW", "MEDIUM", "HIGH"}
_ALLOWED_EVENT_TYPES = {
    "FIRE_DISTRESS",
    "COLLISION_RISK",
    "MAN_OVERBOARD",
    "ORDINARY_COMMUNICATION",
    "UNKNOWN",
}


def build_llm_prompt(task_id: int, transcript_text: str) -> str:
    return (
        "You must analyze marine radio transcript and return JSON only.\\n"
        "Task context:\\n"
        f"- taskId: {task_id}\\n"
        f"- transcriptText: {transcript_text}\\n\\n"
        "Output JSON schema exactly:\\n"
        "{\\n"
        '  "riskLevel": "LOW|MEDIUM|HIGH",\\n'
        '  "eventType": "FIRE_DISTRESS|COLLISION_RISK|MAN_OVERBOARD|ORDINARY_COMMUNICATION|UNKNOWN",\\n'
        '  "eventSummary": "short Chinese summary",\\n'
        '  "reason": "why this level/event",\\n'
        '  "entities": [\\n'
        '    {"entityType": "SHIP_NAME|INCIDENT_TYPE|LOCATION|RESCUE_NEED|CALL_SIGN", "entityValue": "", "confidence": 0.0}\\n'
        "  ]\\n"
        "}\\n"
        "Rules:\\n"
        "1) Return strict JSON only.\\n"
        "2) If uncertain, eventType=UNKNOWN and riskLevel=LOW.\\n"
        "3) confidence in [0,1].\\n"
        "4) entities can be empty list."
    )


def build_correction_prompt(task_id: int, transcript_text: str) -> str:
    return (
        "你是一个语音识别纠错助手，只能做最小必要修改。\\n"
        f"taskId: {task_id}\\n"
        "你的任务：仅修正中文语音转录中的同音字、近音字或明显错字。\\n"
        "严格要求：\\n"
        "1. 只修改错误字词，不允许重写句子。\\n"
        "2. 不改变句式结构。\\n"
        "3. 不改变原意。\\n"
        "4. 不补充信息。\\n"
        "5. 不删除非错误内容。\\n"
        "6. 修改范围越小越好。\\n"
        "7. 如果无法确定，就保持原文不变。\\n"
        "输出要求：仅返回修正后的文本，不要解释，不要 JSON，不要 markdown。\\n"
        f"输入文本：\\n{transcript_text}"
    )


def normalize_correction_content(content: str, original_text: str) -> str:
    raw = (content or "").strip()
    if not raw:
        return original_text
    if raw.startswith("```"):
        raw = raw.replace("```text", "").replace("```", "").strip()
    if raw.startswith('"') and raw.endswith('"') and len(raw) >= 2:
        raw = raw[1:-1]
    return raw.strip() or original_text


def extract_openai_content(data: dict[str, Any]) -> str:
    try:
        choices = data.get("choices") or []
        first = choices[0] if choices else {}
        message = first.get("message") or {}
        content = message.get("content")
        return str(content or "")
    except Exception:
        return ""


def parse_llm_json(content: str) -> dict[str, Any]:
    raw = (content or "").strip()
    if raw.startswith("```"):
        raw = raw.strip("`")
        raw = raw.replace("json", "", 1).strip()

    if not raw.startswith("{"):
        left = raw.find("{")
        right = raw.rfind("}")
        if left >= 0 and right > left:
            raw = raw[left:right + 1]

    try:
        obj = json.loads(raw)
    except Exception:
        logger.warning("LLM JSON parse failed, fallback to default, raw=%s", content)
        return {
            "riskLevel": "LOW",
            "eventType": "UNKNOWN",
            "eventSummary": "模型输出格式异常，已回退默认分析结果。",
            "reason": "LLM 返回非 JSON 或 JSON 解析失败",
            "entities": [],
        }

    risk_level = str(obj.get("riskLevel") or "LOW").upper()
    if risk_level not in _ALLOWED_RISK_LEVELS:
        risk_level = "LOW"

    event_type = str(obj.get("eventType") or "UNKNOWN").upper()
    if event_type not in _ALLOWED_EVENT_TYPES:
        event_type = "UNKNOWN"

    summary = str(obj.get("eventSummary") or "未识别到明确风险事件")
    reason = str(obj.get("reason") or "无")

    entities = obj.get("entities")
    normalized_entities: list[dict[str, Any]] = []
    if isinstance(entities, list):
        for item in entities:
            if not isinstance(item, dict):
                continue
            entity_type = str(item.get("entityType") or "UNKNOWN")
            entity_value = str(item.get("entityValue") or "")
            confidence = safe_confidence(item.get("confidence"))
            if entity_value:
                normalized_entities.append(
                    {
                        "entityType": entity_type,
                        "entityValue": entity_value,
                        "confidence": confidence,
                    }
                )

    return {
        "riskLevel": risk_level,
        "eventType": event_type,
        "eventSummary": summary,
        "reason": reason,
        "entities": normalized_entities,
    }


def safe_confidence(value: Any) -> float | None:
    try:
        num = float(value)
    except Exception:
        return None
    if num < 0:
        return 0.0
    if num > 1:
        return 1.0
    return num
