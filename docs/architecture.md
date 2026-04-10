# coast-radio-guard 架构草图

- backend: Spring Boot 3 + Java 21，负责鉴权、任务管理、告警编排
- frontend: Vue 3 管理端
- ai-service: FastAPI mock SE/ASR/LLM
- redis/mysql: 基础依赖
