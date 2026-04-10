from datetime import datetime, timezone
from fastapi import APIRouter

router = APIRouter()


@router.get("/health")
def health():
    return {
        "service": "coast-radio-guard-ai-service",
        "status": "UP",
        "time": datetime.now(timezone.utc).isoformat(),
    }
