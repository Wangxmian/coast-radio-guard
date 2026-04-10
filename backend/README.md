# Coast Radio Guard Backend

海岸电台智能离线值守系统后端服务，基于 Spring Boot 3 与 Java 21 构建，负责鉴权、业务编排、任务管理、告警闭环、系统配置、历史记录以及 AI 服务接入。

## 模块职责

- 用户认证与授权：基于 Spring Security 与 JWT 提供登录鉴权能力
- 业务编排：统一组织音频任务执行、AI 分析调用与结果回写
- 告警管理：支持人工报警、状态流转、审计日志与闭环处理
- 监控中心支撑：提供概览数据、实时监听状态与任务统计接口
- 配置中心：管理风险阈值、自动报警、VAD / ASR / LLM 开关等系统参数
- AI 服务接入：通过 HTTP 调用 `ai-service`，汇总各阶段的 provider 与 fallback 信息

## 技术栈

- Java 21
- Spring Boot 3.3
- Spring Security
- MyBatis-Plus
- JWT
- MySQL
- Redis

## 启动方式

### 推荐方式

在项目根目录执行：

```bash
./scripts/dev-up.sh
```

该脚本会自动完成数据库初始化，并启动 backend、ai-service 与 frontend。

### 单独启动 Backend

#### 1. 准备依赖服务

```bash
cd ../docker
docker compose -f docker-compose.dev.yml up -d mysql
```

如需单独初始化数据库：

```bash
cd ../backend
mysql -h127.0.0.1 -uroot -proot < sql/init.sql
```

如历史环境尚未执行增量脚本，可按需补充执行：

```bash
cd ../backend
mysql -h127.0.0.1 -uroot -proot coast_radio_guard < sql/upgrade_20260401_monitor_center.sql
mysql -h127.0.0.1 -uroot -proot coast_radio_guard < sql/upgrade_20260402_alarm_flow.sql
mysql -h127.0.0.1 -uroot -proot coast_radio_guard < sql/upgrade_20260403_data_closure.sql
mysql -h127.0.0.1 -uroot -proot coast_radio_guard < sql/upgrade_20260403_transcript_correction.sql
mysql -h127.0.0.1 -uroot -proot coast_radio_guard < sql/upgrade_20260404_realtime_session.sql
```

#### 2. 启动服务

```bash
cd backend
./mvnw spring-boot:run
```

健康检查：

- `GET http://127.0.0.1:18080/api/health`

## 运行配置

主配置文件位于 [src/main/resources/application.yml](/Users/wangxinmian/Downloads/project/coast-radio-guard/backend/src/main/resources/application.yml)。

默认配置摘要：

- 服务端口：`18080`
- MySQL：`127.0.0.1:3306/coast_radio_guard`
- Redis：`127.0.0.1:6379`
- AI 服务地址：`http://127.0.0.1:8000`
- JWT 有效期：`7200` 秒

说明：

- 项目使用仓库内置 `Maven Wrapper`，无需全局 Maven
- 文件上传大小上限已配置为 `20MB`
- Backend 默认通过 `ai.base-url` 调用 AI 服务

## 默认账号

- 用户名：`admin`
- 密码：`Admin@123`

初始化脚本位于 [sql/init.sql](/Users/wangxinmian/Downloads/project/coast-radio-guard/backend/sql/init.sql)。

## 核心接口

### 认证与基础能力

- `GET /api/health`
- `POST /api/auth/login`

### 频道管理

- `GET /api/channels`
- `GET /api/channels/{id}`
- `POST /api/channels`
- `PUT /api/channels/{id}`
- `DELETE /api/channels/{id}`

### 音频任务

- `GET /api/audio-tasks`
- `GET /api/audio-tasks/{id}`
- `POST /api/audio-tasks`
- `PUT /api/audio-tasks/{id}`
- `PUT /api/audio-tasks/{id}/status`
- `POST /api/audio-tasks/{id}/execute`
- `GET /api/audio-tasks/{id}/analysis`
- `DELETE /api/audio-tasks/{id}`

### 监控中心与实时监听

- `GET /api/monitor-center/overview`
- `POST /api/realtime/start`
- `POST /api/realtime/stop`
- `GET /api/realtime/status`
- `POST /api/realtime/chunk`

实时监听链路说明：

- 前端上传短音频片段
- Backend 转发至 AI 服务进行识别
- 前端按轮询方式获取状态与结果

当前实时链路基于分片上传与状态轮询，不依赖 WebSocket 或 SSE。

### 告警中心

- `GET /api/alarms`
- `GET /api/alarms/export`
- `GET /api/alarms/{id}`
- `POST /api/alarms/manual`
- `POST /api/alarms/{id}/ack`
- `POST /api/alarms/{id}/process`
- `POST /api/alarms/{id}/resolve`
- `POST /api/alarms/{id}/close`
- `POST /api/alarms/{id}/false-alarm`

### 历史记录与分析结果

- `GET /api/history-records`
- `GET /api/history-records/export`
- `GET /api/structured-results`
- `GET /api/structured-results/{taskId}`
- `GET /api/structured-results/{taskId}/json`

### 系统配置

- `GET /api/system-configs`
- `GET /api/system-configs/grouped`
- `PUT /api/system-configs`

### 智能体接口

- `POST /api/agent/chat`
- `POST /api/agent/report`

说明：

- `chat` 由 backend 转发 AI 服务对话能力
- `report` 会先聚合业务统计信息，再调用 AI 服务生成摘要
- 返回结果中包含 `providerInfo` 与 `fallbackInfo`，便于前端展示当前实际 provider 状态

## 业务规则说明

### 音频任务执行链路

`POST /api/audio-tasks/{id}/execute` 会组织以下阶段：

- 语音增强
- ASR 转写
- LLM 语义分析
- 结果保存与告警判定

执行结果可返回：

- `providerInfo`
- `stageInfo`
- `fallbackInfo`

这些字段用于标识每个阶段是否命中真实 provider、是否发生 fallback，以及失败原因。

### 系统配置与执行链路联动

在任务执行过程中，以下系统配置会直接影响行为：

- `autoAlarmEnabled=false`：命中规则或 LLM 风险时不自动创建告警
- `riskThreshold`：影响风险等级与规则判定阈值
- `vadEnabled=false`：记录禁用状态，主链路仍可继续执行
- `asrEnabled=false`：直接拒绝执行并返回清晰错误
- `llmEnabled=false`：直接拒绝执行并返回清晰错误

## 接口调用示例

### 1. 登录获取 Token

```bash
curl -X POST http://127.0.0.1:18080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@123"}'
```

### 2. 启动实时监听

```bash
curl -X POST http://127.0.0.1:18080/api/realtime/start \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{"channelId":1,"mode":"manual"}'
```

### 3. 上传音频片段

```bash
curl -X POST http://127.0.0.1:18080/api/realtime/chunk \
  -H 'Authorization: Bearer <token>' \
  -F 'audio=@/path/to/chunk.wav' \
  -F 'taskId=12' \
  -F 'startTime=0' \
  -F 'endTime=3000'
```

### 4. 查询监听状态

```bash
curl http://127.0.0.1:18080/api/realtime/status \
  -H 'Authorization: Bearer <token>'
```

### 5. 停止实时监听

```bash
curl -X POST http://127.0.0.1:18080/api/realtime/stop \
  -H 'Authorization: Bearer <token>'
```

## 开发与联调建议

- 优先使用根目录 `./scripts/dev-up.sh` 完成全链路启动
- 若任务执行失败，先检查 AI 服务健康状态与 `ai.base-url` 配置
- 若执行链路涉及真实 ASR，请确保 `originalFilePath` 或 `enhancedFilePath` 指向本机可读取文件
- 若前端展示 provider 状态异常，重点检查接口响应中的 `providerInfo / stageInfo / fallbackInfo`

## 常见问题

### 1. 登录失败或接口返回 401

优先检查：

- 是否已成功执行 [sql/init.sql](/Users/wangxinmian/Downloads/project/coast-radio-guard/backend/sql/init.sql)
- 前端是否携带了最新 JWT Token
- JWT 配置是否被错误修改

### 2. 任务执行时 ASR 阶段失败

优先检查：

- 音频路径是否真实存在
- AI 服务是否已启动
- AI 服务对应 provider 是否已启用并安装依赖

### 3. 实时监听没有识别结果

优先检查：

- `POST /api/realtime/chunk` 上传的音频格式是否可读
- AI 服务实时接口是否可访问
- 前端是否正在正常轮询 `GET /api/realtime/status`

## 相关文档

- [README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/README.md)
- [../ai-service/README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/ai-service/README.md)
- [../frontend/README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/README.md)
