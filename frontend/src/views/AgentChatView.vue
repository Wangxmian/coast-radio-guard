<template>
  <PageContainer class="crg-page agent-page">
    <div class="page-header-bar">
      <div class="header-left">
        <div class="page-eyebrow">INTELLIGENT AGENT</div>
        <h1 class="page-title">智能体对话 <span class="beta-pill">Beta</span></h1>
        <p class="page-desc">已接入后端意图识别与真实业务数据查询，支持告警统计、任务状态、最新事件和今日摘要。</p>
      </div>
      <div class="agent-status">
        <span class="status-dot" :class="latestFallback ? 'status-dot--pending' : 'status-dot--ok'"></span>
        <span class="status-label">{{ statusText }}</span>
      </div>
    </div>

    <div class="chat-layout">
      <!-- 左侧边栏 -->
      <div class="sidebar">
        <!-- 状态提示 -->
        <div class="sidebar-notice">
          <div class="notice-icon">💡</div>
          <div class="notice-text">当前优先走后端真实数据查询，再按需调用 AI 服务做文本增强；查询失败或数据不足时会自动回退到模板回答。</div>
        </div>

        <!-- 快捷操作 -->
        <div class="sidebar-section">
          <div class="sidebar-section-title">快捷操作</div>
          <div class="quick-grid">
            <button class="quick-card" @click="applyQuickPrompt('today-alarms')">
              <div class="quick-icon quick-icon--blue">🔔</div>
              <div class="quick-body">
                <div class="quick-title">今日告警统计</div>
                <div class="quick-desc">今日告警数量与高风险占比</div>
              </div>
            </button>
            <button class="quick-card" @click="applyQuickPrompt('high-risk')">
              <div class="quick-icon quick-icon--red">⚠️</div>
              <div class="quick-body">
                <div class="quick-title">高风险事件</div>
                <div class="quick-desc">最新 HIGH 风险任务与事件类型</div>
              </div>
            </button>
            <button class="quick-card" @click="applyQuickPrompt('daily-report')">
              <div class="quick-icon quick-icon--green">📊</div>
              <div class="quick-body">
                <div class="quick-title">生成今日日报</div>
                <div class="quick-desc">汇总任务、告警、风险分布</div>
              </div>
            </button>
            <button class="quick-card" @click="applyQuickPrompt('weekly-report')">
              <div class="quick-icon quick-icon--purple">📅</div>
              <div class="quick-body">
                <div class="quick-title">最近一周趋势</div>
                <div class="quick-desc">查看近 7 天告警与高风险变化</div>
              </div>
            </button>
            <button class="quick-card" @click="applyQuickPrompt('analysis-report')">
              <div class="quick-icon quick-icon--orange">📋</div>
              <div class="quick-body">
                <div class="quick-title">未处理告警</div>
                <div class="quick-desc">列出当前仍待处理的告警事项</div>
              </div>
            </button>
          </div>
        </div>

        <!-- 历史会话 -->
        <div class="sidebar-section" v-if="history.length">
          <div class="sidebar-section-title">历史会话</div>
          <el-scrollbar max-height="200px">
            <div class="history-list">
              <button
                v-for="h in history"
                :key="h.id"
                class="history-item"
                @click="restoreHistory(h)"
              >
                <span class="history-icon">💬</span>
                {{ h.title }}
              </button>
            </div>
          </el-scrollbar>
        </div>
      </div>

      <!-- 右侧对话区 -->
      <div class="chat-panel">
        <div class="chat-panel-head">
          <div class="chat-panel-title">对话区域</div>
          <div class="chat-panel-sub">当前 LLM Provider: {{ latestProviderText }}</div>
          <button class="clear-btn" @click="clearChat">清空对话</button>
        </div>

        <!-- 消息列表 -->
        <el-scrollbar ref="scrollRef" class="chat-scroll">
          <div class="chat-list" ref="chatListRef">
            <ChatMessage
              v-for="m in messages"
              :key="m.id"
              :role="m.role"
              :content="m.content"
              :intent="m.intent"
              :source="m.source"
              :fallback="m.fallback"
              :structured-data="m.structuredData"
              :time="m.time"
            />
          </div>
        </el-scrollbar>

        <!-- 输入区 -->
        <div class="input-area">
          <el-input
            ref="inputRef"
            v-model="inputText"
            type="textarea"
            :rows="3"
            placeholder="输入你的问题，例如：今天有哪些高风险告警？（Ctrl+Enter 发送）"
            @keydown.ctrl.enter.prevent="sendMessage"
            resize="none"
            class="chat-input"
          />
          <div class="input-footer">
            <span class="input-hint">Ctrl + Enter 发送</span>
            <el-button type="primary" @click="sendMessage" :disabled="!inputText.trim()">
              发送 ↑
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { agentChat } from '../api/agent'
import { ElMessage } from 'element-plus'
import PageContainer from '../components/PageContainer.vue'
import ChatMessage from '../components/ChatMessage.vue'

let seed = 1
function nextId() {
  seed += 1
  return seed
}

function now() {
  return new Date().toLocaleTimeString()
}

const scrollRef = ref()
const chatListRef = ref()
const inputRef = ref()
const messages = ref([
  {
    id: 1,
    role: 'system',
    content: '智能体最小后端已接入。你可以直接提问，或使用左侧快捷操作。',
    time: now()
  }
])
const inputText = ref('')
const history = ref([])
const latestProvider = ref({})
const latestFallback = ref(false)
const latestFallbackReason = ref('')

const latestProviderText = computed(() => {
  const llm = latestProvider.value?.llm || '-'
  if (latestFallback.value) {
    return `${formatProviderLabel(llm)}（已降级）`
  }
  return formatProviderLabel(llm)
})

const statusText = computed(() => {
  if (!latestProvider.value?.llm) return '等待首个请求'
  if (latestFallback.value) {
    return `已降级：${formatProviderLabel(latestProvider.value.llm)}${latestFallbackReason.value ? `（${latestFallbackReason.value}）` : ''}`
  }
  return `当前模型：${formatProviderLabel(latestProvider.value.llm)}`
})

async function scrollToBottom() {
  await nextTick()
  scrollRef.value?.setScrollTop(chatListRef.value?.scrollHeight || 99999)
}

function pushMessage(role, content) {
  const payload = typeof content === 'string' ? { content } : (content || {})
  messages.value.push({
    id: nextId(),
    role,
    content: payload.content || '',
    intent: payload.intent || '',
    source: payload.source || '',
    fallback: Boolean(payload.fallback),
    structuredData: payload.structuredData ?? null,
    time: payload.time || now()
  })
  scrollToBottom()
}

function clearChat() {
  messages.value = [
    {
      id: nextId(),
      role: 'system',
      content: '会话已清空。你可以继续提问或使用快捷操作。',
      time: now()
    }
  ]
}

function restoreHistory(item) {
  messages.value = item.messages.map((m) => ({ ...m }))
  scrollToBottom()
}

function saveHistory(title) {
  history.value.unshift({
    id: nextId(),
    title,
    messages: messages.value.slice(-6)
  })
  history.value = history.value.slice(0, 8)
}

async function runQuickAction(type) {
  void type
}

const quickPromptMap = {
  'today-alarms': '请统计今天的告警数量，并说明高风险告警占比。',
  'high-risk': '请列出最新的高风险事件，包含任务ID、事件类型和告警状态。',
  'daily-report': '请生成一份今日日报，汇总任务数量、告警数量和风险分布。',
  'weekly-report': '最近一周高风险告警变化趋势怎么样？',
  'analysis-report': '当前有哪些未处理告警？'
}

async function applyQuickPrompt(type) {
  inputText.value = quickPromptMap[type] || ''
  await nextTick()
  inputRef.value?.focus?.()
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return
  pushMessage('user', text)
  inputText.value = ''
  try {
    const resp = await agentChat(text)
    latestProvider.value = resp?.providerInfo || {}
    latestFallback.value = Boolean(resp?.fallbackInfo?.llm?.fallbackUsed)
    latestFallbackReason.value = resp?.fallbackInfo?.llm?.fallbackReason || ''
    pushMessage('assistant', {
      content: resp?.answer || '未返回内容',
      intent: resp?.intent || '',
      source: resp?.source || '',
      fallback: Boolean(resp?.fallback),
      structuredData: resp?.structuredData ?? null
    })
    saveHistory(text)
  } catch (_e) {
    ElMessage.error('智能体对话调用失败')
  }
}

function formatProviderLabel(value) {
  const text = String(value || '').trim()
  if (!text || text === '-') return '-'
  if (text === 'backend-template') return '后端模板'
  if (text === 'mock-llm') return '模拟模型'
  if (text === 'siliconflow') return 'SiliconFlow'
  if (text === 'local') return '本地模型'
  return text
}
</script>

<style scoped>
.agent-page {
  background: #f6f8fc;
}

.beta-pill {
  display: inline-flex;
  align-items: center;
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid #f59e0b;
  color: #b45309;
  background: #fffbeb;
  font-size: 12px;
  font-weight: 700;
  vertical-align: middle;
}

/* ===== 页头 ===== */
.page-header-bar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e8edf5;
}

.page-eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2.5px;
  color: #8b5cf6;
  margin-bottom: 6px;
}

.page-title {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
}

.page-desc {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.agent-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 20px;
  background: #fefce8;
  border: 1px solid #fde68a;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot--pending {
  background: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.2);
}

.status-dot--ok {
  background: #16a34a;
  box-shadow: 0 0 0 3px rgba(22, 163, 74, 0.18);
}

.status-label {
  font-size: 12px;
  font-weight: 600;
  color: #92400e;
}

/* ===== 布局 ===== */
.chat-layout {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 16px;
  align-items: start;
}

/* ===== 左侧边栏 ===== */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sidebar-notice {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  background: #fffbeb;
  border: 1px dashed #fcd34d;
  border-radius: 12px;
}

.notice-icon {
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 1px;
}

.notice-text {
  font-size: 12px;
  color: #78350f;
  line-height: 1.6;
}

.sidebar-section {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e8edf5;
  padding: 16px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.04);
}

.sidebar-section-title {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1.5px;
  color: #94a3b8;
  text-transform: uppercase;
  margin-bottom: 12px;
}

/* 快捷操作卡片 */
.quick-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  background: #fafbfc;
  cursor: pointer;
  text-align: left;
  transition: all 0.2s;
  width: 100%;
}

.quick-card:hover {
  background: #f0f9ff;
  border-color: #bae6fd;
  transform: translateX(2px);
}

.quick-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.quick-icon--blue   { background: #eff6ff; }
.quick-icon--red    { background: #fff1f2; }
.quick-icon--green  { background: #f0fdf4; }
.quick-icon--purple { background: #faf5ff; }
.quick-icon--orange { background: #fff7ed; }

.quick-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 2px;
}

.quick-desc {
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.4;
}

/* 历史会话 */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
  background: #fff;
  color: #475569;
  font-size: 12px;
  text-align: left;
  cursor: pointer;
  transition: all 0.15s;
  width: 100%;
}

.history-item:hover {
  background: #f8faff;
  border-color: #bfdbfe;
  color: #1e293b;
}

.history-icon {
  font-size: 13px;
  flex-shrink: 0;
}

/* ===== 右侧对话面板 ===== */
.chat-panel {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e8edf5;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
  display: flex;
  flex-direction: column;
  min-height: 600px;
}

.chat-panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px 14px;
  border-bottom: 1px solid #f1f5f9;
}

.chat-panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.chat-panel-sub {
  font-size: 12px;
  color: #94a3b8;
  flex: 1;
}

.clear-btn {
  font-size: 12px;
  color: #94a3b8;
  background: none;
  border: 1px solid #e8edf5;
  border-radius: 6px;
  padding: 4px 10px;
  cursor: pointer;
  transition: all 0.15s;
}

.clear-btn:hover {
  color: #ef4444;
  border-color: #fecdd3;
  background: #fff1f2;
}

/* 消息列表 */
.chat-scroll {
  flex: 1;
  background: #fafbfc;
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 16px;
  min-height: 400px;
}

/* 输入区 */
.input-area {
  padding: 14px 16px;
  border-top: 1px solid #f1f5f9;
  background: #fff;
}

.chat-input :deep(.el-textarea__inner) {
  font-size: 14px;
  line-height: 1.6;
  border-radius: 10px;
  border-color: #e8edf5;
  background: #fafbfc;
  resize: none;
  transition: border-color 0.2s;
}

.chat-input :deep(.el-textarea__inner:focus) {
  border-color: #7dd3fc;
  background: #fff;
}

.input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.input-hint {
  font-size: 12px;
  color: #cbd5e1;
}

@media (max-width: 1080px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }
}
</style>
