# Coast Radio Guard

海岸电台智能离线值守系统。项目面向电台音频值守与风险监测场景，提供音频任务编排、语音识别、语义分析、告警闭环、历史追溯与准实时监听能力。

当前仓库已具备本地开发联调所需的完整基础链路，支持通过一键脚本拉起前端、后端、AI 服务及 MySQL 依赖，适合作为日常开发、功能演示和集成验证环境。

## 核心能力

- 音频任务管理：支持任务创建、执行、状态维护与分析详情查看
- AI 分析链路：支持语音增强、ASR 转写、LLM 语义分析与结构化结果输出
- 告警闭环：支持人工报警、告警确认、处置、误报标记、关闭与审计追踪
- 监控中心：支持概览看板、最近告警、准实时监听状态与片段转录展示
- 系统配置：支持风险阈值、自动报警、VAD / ASR / LLM 开关等运行参数维护
- Provider 架构：AI 服务支持 `real / mock / fallback` 切换，便于本地开发与生产接入分层演进

## 系统架构

```text
frontend (Vue 3 + Vite)
        |
        v
backend (Spring Boot 3 + Java 21)
        |
        v
ai-service (FastAPI + provider architecture)
        |
        +-- MySQL 8.4
        +-- Redis 7
        +-- FunASR / GTCRN / SiliconFlow / Mock Providers
```

职责划分：

- `frontend`：管理端界面，负责登录、任务操作、监控中心、告警流转与配置维护
- `backend`：业务编排中心，负责鉴权、任务管理、告警闭环、系统配置、历史记录及 AI 服务接入
- `ai-service`：AI 推理接入层，负责 VAD、语音增强、ASR、LLM 分析与 provider fallback 控制
- `docker`：本地基础依赖编排，目前提供 MySQL 与可选 Redis 容器

## 技术栈

- Frontend: Vue 3, Vite, Element Plus, Pinia, Axios
- Backend: Spring Boot 3.3, Java 21, MyBatis-Plus, Spring Security, JWT, MySQL, Redis
- AI Service: FastAPI, Uvicorn, provider-based service factory
- Infra: Docker Compose, Maven Wrapper, Python venv

## 目录结构

```text
coast-radio-guard/
├── backend/                 Spring Boot 后端
├── frontend/                Vue 3 前端
├── ai-service/              FastAPI AI 服务
├── docker/                  本地依赖编排
├── docs/                    架构与补充文档
├── scripts/                 一键启动、状态检查、停止脚本
├── logs/                    运行日志目录（脚本启动后生成）
└── .run/                    PID 文件目录（脚本启动后生成）
```

## 运行环境

建议在 macOS / Linux 开发环境下运行，并预先准备以下依赖：

- JDK 21
- Node.js 与 npm
- Python 3（需支持 `venv`）
- Docker 与 Docker Compose v2

说明：

- 后端使用仓库内置 `Maven Wrapper`，无需额外安装 Maven
- 前端开发默认通过 Vite 代理访问后端与 AI 服务
- AI 服务可运行在纯 mock 模式，也可按需启用真实模型与外部 LLM

## 快速开始

### 方式一：一键启动（推荐）

在项目根目录执行：

```bash
./scripts/dev-up.sh
```

脚本默认会完成以下动作：

- 启动 MySQL 容器
- 等待数据库就绪并自动执行 [backend/sql/init.sql](/Users/wangxinmian/Downloads/project/coast-radio-guard/backend/sql/init.sql)
- 启动后端服务
- 启动 AI 服务
- 创建 AI 服务虚拟环境并安装基础依赖
- 启动前端服务
- 在 MySQL 数据卷异常时自动尝试重建并重试

默认访问地址：

- Frontend: `http://127.0.0.1:5173`
- Backend Health: `http://127.0.0.1:18080/api/health`
- AI Service Health: `http://127.0.0.1:8000/api/health`

常用辅助命令：

```bash
# 查看进程与容器状态
./scripts/dev-status.sh

# 停止本地开发环境
./scripts/dev-down.sh
```

### 方式二：分模块启动

#### 1. 启动基础依赖

```bash
cd docker
docker compose -f docker-compose.dev.yml up -d
```

默认端口：

- MySQL: `3306`
- Redis: `6379`

说明：

- `dev-up.sh` 默认只拉起 MySQL
- Redis 在部分场景下复用本机 `6379`，若需要项目自带 Redis 容器，可手动执行上面的 compose 命令

#### 2. 启动 Backend

```bash
cd backend
./mvnw spring-boot:run
```

配置位置：[backend/src/main/resources/application.yml](/Users/wangxinmian/Downloads/project/coast-radio-guard/backend/src/main/resources/application.yml)

默认配置：

- 端口：`18080`
- 数据库：`jdbc:mysql://127.0.0.1:3306/coast_radio_guard`
- Redis：`127.0.0.1:6379`
- AI 服务地址：`http://127.0.0.1:8000`

#### 3. 启动 AI Service

```bash
cd ai-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

# 按需安装真实 provider 依赖
# pip install -r requirements-real.txt
# pip install -r requirements-gtcrn.txt

cp .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

#### 4. 启动 Frontend

```bash
cd frontend
npm install
npm run dev
```

前端默认配置通过 [frontend/vite.config.js](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/vite.config.js) 代理：

- `/backend-api` -> `http://127.0.0.1:18080/api`
- `/ai-api` -> `http://127.0.0.1:8000/api`

如果需要绕过代理，可通过 `VITE_API_BASE_URL` 指定后端基础地址。

## 默认账号

- 用户名：`admin`
- 密码：`Admin@123`

登录页位于 [frontend/src/views/LoginView.vue](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/src/views/LoginView.vue)。

## AI Provider 说明

AI 服务采用 provider 架构，支持真实能力、mock 能力与自动 fallback。

当前支持：

- VAD: `funasr` / `mock`
- SE: `gtcrn` / `mock`
- ASR: `funasr` / `mock`
- LLM: `siliconflow` / `local` / `mock`

常用环境变量位于 [ai-service/.env.example](/Users/wangxinmian/Downloads/project/coast-radio-guard/ai-service/.env.example)，重点包括：

- `AI_USE_REAL_VAD`
- `AI_USE_REAL_ASR`
- `AI_USE_REAL_SE`
- `AI_USE_REAL_LLM`
- `AI_ALLOW_PROVIDER_FALLBACK`
- `VAD_PROVIDER`
- `ASR_PROVIDER`
- `SE_PROVIDER`
- `LLM_PROVIDER`
- `SILICONFLOW_API_KEY`

环境变量优先级：

- shell / process 环境变量
- `.env`
- 代码默认值

说明：

- 若 `AI_ALLOW_PROVIDER_FALLBACK=true`，真实 provider 不可用时会自动降级到 mock
- 若启用真实 LLM，请显式配置 `SILICONFLOW_API_KEY`
- 若启用 GTCRN，请确保 `GTCRN_PROJECT_DIR` 与 checkpoint 目录配置正确

## 当前功能范围

### 已联通能力

- 登录鉴权
- 频道管理
- 音频任务管理与执行
- 分析详情查看
- 监控中心概览
- 准实时音频片段上传与轮询式转录
- 告警中心与告警流转
- 历史记录查询与导出
- 分析报告与结构化结果查看
- 系统参数读取与保存
- 智能体对话最小能力

### 实时能力说明

当前实现的是基于分片上传与状态轮询的准实时方案：

- 前端上传短音频片段
- Backend 转发至 AI Service
- 前端轮询获取状态与结果

当前尚不是 WebSocket / SSE 真流式音频链路，后续可在现有接口基础上继续演进。

## 主要接口概览

### Backend

- `GET /api/health`
- `POST /api/auth/login`
- `GET /api/channels`
- `POST /api/audio-tasks`
- `POST /api/audio-tasks/{id}/execute`
- `GET /api/audio-tasks/{id}/analysis`
- `GET /api/monitor-center/overview`
- `GET /api/system-configs`
- `PUT /api/system-configs`
- `GET /api/history-records`
- `GET /api/structured-results`
- `GET /api/alarms`
- `POST /api/alarms/manual`
- `POST /api/realtime/start`
- `POST /api/realtime/chunk`
- `GET /api/realtime/status`
- `POST /api/realtime/stop`
- `POST /api/agent/chat`
- `POST /api/agent/report`

### AI Service

- `GET /api/health`
- `POST /api/se/enhance`
- `POST /api/asr/transcribe`
- `POST /api/llm/analyze`
- `POST /api/speech/analyze`
- `POST /api/realtime/transcribe`
- `GET /api/debug/providers`

更完整的接口说明请参考：

- [backend/README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/backend/README.md)
- [ai-service/README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/ai-service/README.md)
- [frontend/README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/README.md)

## 开发建议

- 首次接手项目时优先使用 `./scripts/dev-up.sh`，先验证整体链路可用
- 修改 AI provider 配置后，建议查看 `logs/ai-service.log` 确认 effective provider 与 fallback 信息
- 联调前端时优先走 Vite 代理，避免 `localhost / 127.0.0.1` 混用导致跨域或网络异常
- 若仅验证业务编排链路，可先使用 mock provider，待流程稳定后再接入真实模型

## 日志与运行文件

- 运行日志目录：`logs/`
- PID 文件目录：`.run/`

典型日志文件：

- `logs/backend.log`
- `logs/ai-service.log`
- `logs/frontend.log`

## 常见问题排查

### 1. `dev-up.sh` 启动失败

优先检查：

- Docker 是否正常运行
- `3306`、`5173`、`8000`、`18080` 端口是否被占用
- 本机是否已存在冲突的 Redis / MySQL 实例

### 2. Backend 能启动但登录或任务执行失败

优先检查：

- 数据库是否已成功初始化
- AI 服务健康检查是否通过：`http://127.0.0.1:8000/api/health`
- [backend/src/main/resources/application.yml](/Users/wangxinmian/Downloads/project/coast-radio-guard/backend/src/main/resources/application.yml) 中的 `ai.base-url` 是否正确

### 3. AI 服务落到 mock provider

优先检查：

- 是否安装了真实 provider 依赖
- `.env` 中开关是否开启
- 模型路径是否存在
- `SILICONFLOW_API_KEY` 是否已正确配置
- `GET /api/debug/providers` 返回的 `effectiveProvider` 与 `fallbackUsed`

### 4. 前端出现 Network Error

优先检查：

- 前端是否通过 Vite 代理访问后端
- Backend 与 AI Service 是否已启动
- 是否混用了 `localhost` 与 `127.0.0.1`

## 后续演进方向

- 真流式音频接入与推送机制
- 更完整的智能体工具调用与报告编排
- 更细粒度的任务调度与监控指标体系
- 模型接入的可观测性与配置中心化

## 相关文档

- [docs/architecture.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/docs/architecture.md)
- [backend/README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/backend/README.md)
- [frontend/README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/README.md)
- [ai-service/README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/ai-service/README.md)
