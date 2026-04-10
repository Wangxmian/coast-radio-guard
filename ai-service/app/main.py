import logging

from fastapi import FastAPI

from app.routers import asr, debug, health, llm, realtime, se, speech
from app.services.service_factory import get_settings, warmup_services

settings = get_settings()

logging.basicConfig(
    level=settings.app_log_level,
    format='%(asctime)s [%(levelname)s] %(name)s - %(message)s',
)

app = FastAPI(title='coast-radio-guard-ai-service', version='0.2.1')


@app.on_event('startup')
def on_startup() -> None:
    warmup_services()


# 兼容路径
app.include_router(health.router, prefix='/api', tags=['health'])
app.include_router(debug.router, prefix='/api/debug', tags=['debug'])
app.include_router(se.router, prefix='/api/se', tags=['se'])
app.include_router(asr.router, prefix='/api/asr', tags=['asr'])
app.include_router(realtime.router, prefix='/api/realtime', tags=['realtime'])
app.include_router(llm.router, prefix='/api/llm', tags=['llm'])
app.include_router(speech.router, prefix='/api/speech', tags=['speech'])

# 新路径
app.include_router(debug.router, prefix='/debug', tags=['debug-v2'])
app.include_router(se.router, prefix='/se', tags=['se-v2'])
app.include_router(asr.router, prefix='/asr', tags=['asr-v2'])
app.include_router(realtime.router, prefix='/realtime', tags=['realtime-v2'])
app.include_router(llm.router, prefix='/llm', tags=['llm-v2'])
app.include_router(speech.router, prefix='/speech', tags=['speech-v2'])
