# Coast Radio Guard AI Service

海岸电台智能离线值守系统 AI 服务，基于 FastAPI 构建，负责语音增强、VAD、ASR、LLM 分析、实时片段转录以及 provider 选择与 fallback 控制。

## 模块职责

- 统一封装语音增强、语音识别与语义分析能力
- 支持真实 provider、mock provider 与自动 fallback
- 为 backend 提供离线任务分析接口
- 为实时监听链路提供片段转录接口
- 输出 provider、stage 与 fallback 诊断信息，便于联调与问题定位

## 技术栈

- Python 3
- FastAPI
- Uvicorn
- Provider-based service architecture

## 支持的 Provider

### VAD

- `funasr`
- `mock`

### SE

- `gtcrn`
- `mock`

### ASR

- `funasr`
- `mock`

### LLM

- `siliconflow`
- `local`
- `mock`

## 安装与启动

```bash
cd ai-service
python3 -m venv .venv
source .venv/bin/activate

pip install -r requirements.txt

# 按需安装真实 provider 依赖
# pip install -r requirements-real.txt

# 启用 GTCRN 时安装
# pip install -r requirements-gtcrn.txt

cp .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

健康检查：

- `GET http://127.0.0.1:8000/api/health`

## 配置说明

环境变量模板位于 [/.env.example](/Users/wangxinmian/Downloads/project/coast-radio-guard/ai-service/.env.example)。

常用配置项如下：

```env
AI_USE_REAL_VAD=true
AI_USE_REAL_ASR=true
AI_USE_REAL_SE=true
AI_USE_REAL_LLM=true
AI_ALLOW_PROVIDER_FALLBACK=true

VAD_PROVIDER=funasr
ASR_PROVIDER=funasr
SE_PROVIDER=gtcrn
LLM_PROVIDER=siliconflow

FUNASR_VAD_MODEL=iic/speech_fsmn_vad_zh-cn-16k-common-pytorch
FUNASR_VAD_MODEL_DIR=
FUNASR_ASR_MODEL=iic/SenseVoiceSmall
FUNASR_ASR_MODEL_DIR=

SILICONFLOW_API_KEY=
SILICONFLOW_BASE_URL=https://api.siliconflow.cn/v1
SILICONFLOW_MODEL=deepseek-ai/DeepSeek-V3.2

GTCRN_PROJECT_DIR=/Users/wangxinmian/Downloads/project/gtcrn-main
GTCRN_CHECKPOINT_DIR=/Users/wangxinmian/Downloads/project/gtcrn-main/checkpoints
GTCRN_CHECKPOINT_FILE=model_trained_on_dns3.tar
GTCRN_DEVICE=cpu
GTCRN_OUTPUT_DIR=./data/enhanced
```

环境变量优先级：

- shell / process 环境变量
- `.env`
- 默认值

说明：

- `scripts/dev-up.sh` 不会直接 `source .env`
- shell 中显式设置的非空环境变量不会被 `.env` 覆盖
- 当 `AI_ALLOW_PROVIDER_FALLBACK=true` 时，真实 provider 不可用会自动回退到 mock provider
- 启动日志中的 `llm key check` 仅显示 `set / empty`，不会打印完整密钥

## 模型准备

如需启用真实 FunASR 模型，可通过 ModelScope 预下载模型：

```bash
pip install modelscope
modelscope download --model iic/speech_fsmn_vad_zh-cn-16k-common-pytorch
modelscope download --model iic/SenseVoiceSmall
```

Python SDK 示例：

```python
from modelscope import snapshot_download

vad_dir = snapshot_download('iic/speech_fsmn_vad_zh-cn-16k-common-pytorch')
asr_dir = snapshot_download('iic/SenseVoiceSmall')
```

建议将模型预先下载到本地，再通过以下变量指定目录：

- `FUNASR_VAD_MODEL_DIR`
- `FUNASR_ASR_MODEL_DIR`

## GTCRN 配置说明

如需启用 GTCRN 语音增强，请确保以下目录存在：

- `GTCRN_PROJECT_DIR`
- `GTCRN_CHECKPOINT_DIR`

同时建议配置：

- `AI_USE_REAL_SE=true`
- `SE_PROVIDER=gtcrn`

成功加载时，日志会输出类似信息：

- `GTCRN provider loaded`
- `GTCRN enhance success`

若加载失败：

- `AI_ALLOW_PROVIDER_FALLBACK=true` 时会自动回退到 `mock-se`
- `AI_ALLOW_PROVIDER_FALLBACK=false` 时会直接返回错误

## 核心接口

### 健康检查

- `GET /api/health`

### 语音增强

- `POST /se/enhance`
- `POST /api/se/enhance`

### ASR 转写

- `POST /asr/transcribe`
- `POST /api/asr/transcribe`

### LLM 分析

- `POST /llm/analyze`
- `POST /api/llm/analyze`

### 综合语音分析

- `POST /speech/analyze`
- `POST /api/speech/analyze`

### 实时片段转录

- `POST /realtime/transcribe`
- `POST /api/realtime/transcribe`

请求格式：`multipart/form-data`

字段说明：

- `audio`：必填，音频片段文件
- `taskId`：可选
- `startTime`：可选
- `endTime`：可选

主要返回字段：

- `transcriptText`
- `language`
- `providerInfo`
- `stageInfo`
- `fallbackInfo`
- `isFinal`
- `timestamp`

说明：

- 该接口供 backend 的 `/api/realtime/chunk` 调用
- 用于分片音频识别与监听状态展示
- 不影响离线任务执行链路

### 调试接口

- `GET /api/debug/providers`
- `POST /api/debug/llm-check`

## 处理链路说明

### 离线任务链路

由 backend 的 `POST /api/audio-tasks/{id}/execute` 触发，典型流程如下：

1. 语音增强
2. ASR 转写
3. LLM 分析
4. 结果返回与状态落库

说明：

- 若使用真实 ASR，`originalFilePath` 或 `enhancedFilePath` 必须指向真实可读取文件
- 若文件路径不存在，将在 ASR 阶段返回清晰错误

### 实时转录链路

由 backend 组织调用：

1. `POST /api/realtime/start`
2. `POST /api/realtime/chunk`
3. `GET /api/realtime/status`
4. `POST /api/realtime/stop`

AI 服务侧实际处理接口为 `/realtime/transcribe`。

## 如何确认真实 Provider 生效

建议从以下几个角度确认：

1. 查看启动日志中的 provider summary
2. 调用 `GET /api/debug/providers`
3. 调用 `POST /api/debug/llm-check`
4. 检查业务接口返回中的 `providerInfo / stageInfo / fallbackInfo`

常见判定逻辑：

- 若 `effectiveProvider` 为 mock provider，说明当前已降级
- 若 `fallbackUsed=false` 且 `effectiveProvider` 为目标真实 provider，说明真实能力已生效

## Fallback 行为说明

- `AI_ALLOW_PROVIDER_FALLBACK=true`：真实 provider 不可用时自动降级，并记录原因
- `AI_ALLOW_PROVIDER_FALLBACK=false`：直接返回错误，不执行静默降级

常见 fallback 原因：

- 未安装 `torch`、`torchaudio` 等推理依赖
- 模型目录不存在或配置错误
- `SILICONFLOW_API_KEY` 未配置
- GTCRN checkpoint 缺失
- 本机缺少音频处理依赖

## 开发建议

- 联调阶段优先确认 `GET /api/health` 与 `GET /api/debug/providers`
- 首次启用真实 provider 时，先单独启动 AI 服务观察日志
- 若需要稳定演示环境，可保留 fallback 开关开启
- 若需要严格验证真实能力，可关闭 fallback 以便尽早暴露配置问题

## 常见问题

### 1. LLM 一直回退到 mock

优先检查：

- `SILICONFLOW_API_KEY` 是否已设置
- `LLM_PROVIDER` 是否为 `siliconflow`
- `AI_ALLOW_PROVIDER_FALLBACK` 是否开启

### 2. GTCRN 无法加载

优先检查：

- `GTCRN_PROJECT_DIR` 是否正确
- `GTCRN_CHECKPOINT_DIR` 是否存在
- 是否已安装 [requirements-gtcrn.txt](/Users/wangxinmian/Downloads/project/coast-radio-guard/ai-service/requirements-gtcrn.txt)

### 3. ASR 识别失败

优先检查：

- FunASR 模型目录是否存在
- 音频文件路径是否可读
- 是否已安装真实 ASR 依赖

## 相关文档

- [README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/README.md)
- [../backend/README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/backend/README.md)
- [../frontend/README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/README.md)
