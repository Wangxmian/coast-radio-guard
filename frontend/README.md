# Coast Radio Guard Frontend

海岸电台智能离线值守平台前端，基于 Vue 3、Vite 与 Element Plus 构建，负责系统管理界面、任务操作、监控中心、告警处理与配置维护。

## 模块职责

- 用户登录与会话维持
- 值守监控中心展示
- 频道、任务、历史记录与分析报告管理
- 告警中心操作与状态流转
- 系统配置维护
- 智能体对话入口与结果展示

## 技术栈

- Vue 3
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios

## 启动方式

```bash
cd frontend
npm install
npm run dev
```

默认访问地址：

- `http://127.0.0.1:5173`

构建命令：

```bash
npm run build
```

## 运行配置

Vite 配置位于 [vite.config.js](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/vite.config.js)。

开发环境默认代理：

- `/backend-api` -> `http://127.0.0.1:18080/api`
- `/ai-api` -> `http://127.0.0.1:8000/api`

前端请求封装位于 [src/api/request.js](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/src/api/request.js)，默认优先走 Vite 代理。

如需显式指定后端地址，可设置环境变量：

- `VITE_API_BASE_URL`

说明：

- 若设置 `VITE_API_BASE_URL`，请求地址会切换为 `${VITE_API_BASE_URL}/api`
- 若未设置，默认走 `/backend-api`

## 默认账号

- 用户名：`admin`
- 密码：`Admin@123`

登录页位于 [src/views/LoginView.vue](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/src/views/LoginView.vue)。

## 页面结构

路由定义位于 [src/router/index.js](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/src/router/index.js)。

当前页面结构如下：

- 值守监控中心：`/monitor-center`
- 频道管理：`/channels`
- 音频任务：`/audio-tasks`
- 任务分析详情：`/audio-tasks/:id`
- 历史记录：`/history-records`
- 分析报告：`/analysis-reports`
- 告警中心：`/alarms`
- 智能体对话：`/agent-chat`
- 系统设置：`/system-settings`

兼容路由：

- `/dashboard` 重定向到 `/monitor-center`
- `/structured-results` 重定向到 `/analysis-reports`

## 主要页面说明

### 值守监控中心

页面位于 [src/views/MonitorCenterView.vue](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/src/views/MonitorCenterView.vue)。

主要展示内容：

- 当日任务、告警、高风险事件与频道概览
- 最近告警列表
- 实时监听状态
- 当前频道与片段转录信息
- 时间轴与人工报警入口

依赖接口：

- `GET /api/monitor-center/overview`
- `POST /api/realtime/start`
- `POST /api/realtime/chunk`
- `GET /api/realtime/status`
- `POST /api/realtime/stop`
- `POST /api/alarms/manual`

### 音频任务与分析详情

页面位于：

- [src/views/AudioTasksView.vue](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/src/views/AudioTasksView.vue)
- [src/views/AudioTaskDetailView.vue](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/src/views/AudioTaskDetailView.vue)

主要能力：

- 任务列表与筛选
- 任务创建、编辑、执行
- 执行链路状态查看
- provider、stage、fallback 信息展示
- 分析详情与结果查看

### 历史记录与分析报告

页面位于：

- [src/views/HistoryRecordsView.vue](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/src/views/HistoryRecordsView.vue)
- [src/views/StructuredResultsView.vue](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/src/views/StructuredResultsView.vue)

主要能力：

- 历史记录查询与导出
- 分析报告检索
- 结构化结果查看
- JSON 视图展示

### 告警中心

页面位于 [src/views/AlarmsView.vue](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/src/views/AlarmsView.vue)。

主要能力：

- 告警列表查看
- 告警状态流转
- 审计日志查看
- 人工报警与告警处理

### 系统设置

页面位于 [src/views/SystemSettingsView.vue](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/src/views/SystemSettingsView.vue)。

主要能力：

- 热词词典配置
- 风险阈值配置
- 自动报警配置
- 模型能力说明展示
- VAD / ASR / LLM 开关维护

依赖接口：

- `GET /api/system-configs`
- `PUT /api/system-configs`

### 智能体对话

页面位于 [src/views/AgentChatView.vue](/Users/wangxinmian/Downloads/project/coast-radio-guard/frontend/src/views/AgentChatView.vue)。

已接入接口：

- `POST /api/agent/chat`
- `POST /api/agent/report`

页面会展示当前 provider 与 fallback 状态，用于辅助判断 LLM 是否走真实服务。

## 联调范围

当前前端已对接以下业务能力：

- 登录鉴权
- 监控中心概览
- 实时监听状态与片段转录
- 频道管理
- 音频任务管理
- 任务分析详情
- 告警流转与审计日志
- 历史记录查询与导出
- 分析报告查询与 JSON 展示
- 系统设置读取与保存
- 智能体对话与报告摘要

当前实时监听采用分片上传与状态轮询方式，不依赖 WebSocket 或 SSE。

## LLM 状态判断说明

前端不直接调用 SiliconFlow，LLM 实际执行情况取决于 backend 与 ai-service 返回的数据。

建议检查以下字段：

1. `providerInfo`
2. `stageInfo`
3. `fallbackInfo`

判定方法：

- 若 `stageInfo.llm.effectiveProvider=mock-llm`，说明当前为降级 provider
- 若 `stageInfo.llm.effectiveProvider=siliconflow` 且 `fallbackUsed=false`，说明已调用真实 LLM

## 开发建议

- 本地联调优先配合根目录 `./scripts/dev-up.sh`
- 保持前端、后端、AI 服务统一使用 `127.0.0.1`
- 遇到 `Network Error` 时，优先检查 Vite 代理与后端服务状态
- 如需确认接口路径，优先查看 `src/api` 目录下的请求封装

## 常见问题

### 1. 登录后跳回登录页

优先检查：

- 后端登录接口是否正常返回 JWT
- 浏览器本地存储中的 `crg_token` 是否存在
- 是否触发了 401 自动清理逻辑

### 2. 页面出现 Network Error

优先检查：

- Backend 是否运行在 `18080`
- AI 服务是否运行在 `8000`
- Vite 代理是否生效
- 是否混用了 `localhost` 与 `127.0.0.1`

### 3. 页面显示 mock provider

优先检查：

- 后端返回的 `providerInfo / stageInfo / fallbackInfo`
- AI 服务的 provider 配置是否正确
- 是否缺少真实模型依赖或 API Key

## 相关文档

- [README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/README.md)
- [../backend/README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/backend/README.md)
- [../ai-service/README.md](/Users/wangxinmian/Downloads/project/coast-radio-guard/ai-service/README.md)
