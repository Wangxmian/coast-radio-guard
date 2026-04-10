<template>
  <PageContainer class="crg-page monitor-page" v-loading="loading">
    <div class="page-header-bar">
      <div class="header-left">
        <div class="page-eyebrow">WATCH CENTER</div>
        <h1 class="page-title">值守监控中心</h1>
        <p class="page-desc">态势感知 + 实时转录模式。当前链路为最小可用版本（短音频片段准实时识别）。</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadOverview">刷新态势</el-button>
      </div>
    </div>

    <div class="overview-grid">
      <div class="metric-card">
        <div class="metric-label">今日任务</div>
        <div class="metric-value">{{ overview.todayTaskCount ?? 0 }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">今日告警</div>
        <div class="metric-value">{{ overview.todayAlarmCount ?? 0 }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">高风险事件</div>
        <div class="metric-value">{{ overview.todayHighRiskEventCount ?? 0 }}</div>
      </div>
      <div class="metric-card">
        <div class="metric-label">频道数</div>
        <div class="metric-value">{{ overview.channelCount ?? 0 }}</div>
      </div>
      <div class="metric-card metric-status">
        <div class="metric-label">在线状态</div>
        <div class="metric-value metric-value--small">{{ overview.systemStatus || 'UNKNOWN' }}</div>
        <div class="metric-hint">{{ nowText }}</div>
      </div>
    </div>

    <div v-if="latestAlarm" class="alert-banner">
      <div class="alert-banner__content">
        <div class="alert-banner__title">最新高风险告警 #{{ latestAlarm.id }}</div>
        <div class="alert-banner__text">{{ latestAlarm.triggerReason || '监控中心检测到新的高风险告警' }}</div>
      </div>
      <div class="alert-banner__actions">
        <el-button size="small" type="danger" @click="goAlarm(latestAlarm.id)">前往告警中心</el-button>
        <el-button size="small" @click="goTask(realtimeStatus.currentTaskId || latestAlarm.taskId)">任务详情</el-button>
      </div>
    </div>

    <SectionPanel title="最近告警" subtitle="来自 /api/monitor-center/overview" body-padding="12px 14px 14px">
      <div class="table-scroll">
      <el-table :data="overview.recentAlarms || []" size="small" empty-text="暂无告警" class="compact-table">
        <el-table-column prop="id" label="告警ID" width="90" />
        <el-table-column prop="taskId" label="任务ID" width="90" />
        <el-table-column label="等级" width="100">
          <template #default="scope">
            <RiskTag :level="scope.row.alarmLevel" />
          </template>
        </el-table-column>
        <el-table-column prop="triggerSource" label="来源" width="120" />
        <el-table-column prop="triggerReason" label="原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <StatusTag :status="scope.row.alarmStatus" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" min-width="160" />
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <div class="alarm-actions">
              <el-button size="small" text type="primary" @click="goAlarm(scope.row.id)">告警</el-button>
              <el-button size="small" text @click="goTask(scope.row.taskId)">任务</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      </div>
    </SectionPanel>

    <SectionPanel title="实时监控（实时转录模式）" subtitle="最小可用：启动监听 + 发送音频片段 + 轮询状态" body-padding="14px">
      <div class="realtime-metrics">
        <div class="rt-card">
          <div class="rt-label">监听状态</div>
          <div class="rt-value">{{ realtimeStatus.listeningStatus || 'IDLE' }}</div>
          <div class="rt-hint">{{ realtimeStatus.note || '-' }}</div>
        </div>
        <div class="rt-card">
          <div class="rt-label">当前频道</div>
          <div class="rt-value">{{ currentChannelName }}</div>
          <div class="rt-hint">ID: {{ realtimeStatus.currentChannel || '-' }}</div>
        </div>
        <div class="rt-card">
          <div class="rt-label">当前时间</div>
          <div class="rt-value rt-time">{{ nowText }}</div>
          <div class="rt-hint">本地监控时间</div>
        </div>
        <div class="rt-card rt-card--alarm">
          <div class="rt-label">手动报警</div>
          <el-button type="danger" size="small" :loading="manualAlarmLoading" @click="manualAlarm">触发报警</el-button>
          <div class="rt-hint">触发来源 MANUAL</div>
        </div>
      </div>

      <div class="control-row">
        <el-input-number v-model="realtimeControl.channelId" :min="1" :step="1" size="small" style="width: 120px" />
        <el-button type="primary" size="small" :loading="starting" @click="startListening">启动监听</el-button>
        <el-button size="small" :loading="stopping" @click="stopListening">停止监听</el-button>
        <el-input-number v-model="realtimeControl.taskId" :min="1" :step="1" size="small" style="width: 120px" placeholder="taskId" />
        <label class="file-btn">
          <input ref="chunkInputRef" type="file" accept="audio/*" @change="onChunkSelected" class="file-input" />
          <span>{{ selectedChunkFile ? selectedChunkFile.name : '选择音频片段' }}</span>
        </label>
        <el-button type="success" size="small" :loading="sendingChunk" :disabled="!selectedChunkFile" @click="sendChunk">发送片段</el-button>
      </div>

      <div class="provider-note" v-if="Object.keys(realtimeStatus.providerInfo || {}).length">
        providerInfo: {{ JSON.stringify(realtimeStatus.providerInfo) }}
      </div>

      <div class="realtime-link-bar">
        <div class="link-chip">当前任务 #{{ realtimeStatus.currentTaskId || '-' }}</div>
        <div class="link-chip">最近风险 {{ realtimeStatus.latestRiskLevel || '-' }}</div>
        <div class="link-chip">事件类型 {{ realtimeStatus.latestEventType || '-' }}</div>
        <el-button size="small" text type="primary" :disabled="!realtimeStatus.currentTaskId" @click="goTask(realtimeStatus.currentTaskId)">任务详情</el-button>
        <el-button size="small" text :disabled="!realtimeStatus.currentTaskId" @click="goHistory(realtimeStatus.currentTaskId)">历史记录</el-button>
        <el-button size="small" text :disabled="!realtimeStatus.currentTaskId" @click="goAnalysis(realtimeStatus.currentTaskId)">分析报告</el-button>
        <el-button size="small" text :disabled="!realtimeStatus.latestAlarmId" @click="goAlarm(realtimeStatus.latestAlarmId)">关联告警</el-button>
      </div>

      <div class="realtime-grid">
        <div class="sub-panel">
          <div class="sub-panel-title">当前转录内容</div>
          <div class="transcript-box">{{ realtimeStatus.currentTranscript || '暂无实时转录内容。请先启动监听并发送音频片段。' }}</div>
        </div>
        <div class="sub-panel">
          <div class="sub-panel-title">时间轴</div>
          <TimelinePanel :items="timelineItems" empty-text="暂无转录片段" />
        </div>
      </div>
    </SectionPanel>
  </PageContainer>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageContainer from '../components/PageContainer.vue'
import SectionPanel from '../components/SectionPanel.vue'
import StatusTag from '../components/StatusTag.vue'
import RiskTag from '../components/RiskTag.vue'
import TimelinePanel from '../components/TimelinePanel.vue'
import { createManualAlarm } from '../api/alarm'
import { getMonitorCenterOverview } from '../api/monitorCenter'
import { getChannels } from '../api/channel'
import { getRealtimeStatus, sendRealtimeChunk, startRealtime, stopRealtime } from '../api/realtime'

const loading = ref(false)
const router = useRouter()
const starting = ref(false)
const stopping = ref(false)
const sendingChunk = ref(false)
const manualAlarmLoading = ref(false)
const selectedChunkFile = ref(null)
const chunkInputRef = ref()
const channels = ref([])
const nowText = ref(new Date().toLocaleString())

let pollTimer = null
let overviewTimer = null
const clockTimer = setInterval(() => {
  nowText.value = new Date().toLocaleString()
}, 1000)

const overview = reactive({
  systemStatus: 'ONLINE',
  todayTaskCount: 0,
  todayAlarmCount: 0,
  todayHighRiskEventCount: 0,
  channelCount: 0,
  recentAlarms: []
})

const realtimeControl = reactive({
  channelId: 1,
  mode: 'manual',
  taskId: null
})

const realtimeStatus = reactive({
  listeningStatus: 'IDLE',
  streamStatus: 'NOT_CONNECTED',
  currentChannel: null,
  currentTaskId: null,
  currentTranscript: null,
  latestRiskLevel: '',
  latestEventType: '',
  latestAlarmId: null,
  latestAlarmLevel: '',
  latestAlarmStatus: '',
  latestAlarmReason: '',
  recentSegments: [],
  providerInfo: {},
  note: ''
})

function looksLikeMojibake(text) {
  return /[ÃÂÐÑØæåäçéèêëîïôöûüß]/.test(String(text || '')) || String(text || '').includes('�')
}

function formatChannelLabel(channel, id) {
  if (!channel) {
    return id ? `频道 #${id}` : '未选择频道'
  }
  const name = String(channel.channelName || '').trim()
  const code = String(channel.channelCode || '').trim()
  if (name && !looksLikeMojibake(name)) {
    return name
  }
  if (code) {
    return looksLikeMojibake(name) ? `${code}（频道名称异常）` : code
  }
  return id ? `频道 #${id}` : '未选择频道'
}

const currentChannelName = computed(() => {
  const id = realtimeStatus.currentChannel || realtimeControl.channelId
  const ch = channels.value.find((x) => Number(x.id) === Number(id))
  return formatChannelLabel(ch, id)
})

const latestAlarm = computed(() => {
  const alarms = Array.isArray(overview.recentAlarms) ? overview.recentAlarms : []
  return alarms.find((item) => String(item.alarmLevel || '').toUpperCase() === 'HIGH') || alarms[0] || null
})

const timelineItems = computed(() => {
  const segments = Array.isArray(realtimeStatus.recentSegments) ? realtimeStatus.recentSegments : []
  return segments.map((item, idx) => ({
    id: item.segmentId || `${idx}`,
    time: formatSegmentTime(item),
    content: item.transcriptText || '(空转录)'
  }))
})

function applyRealtimeStatus(data) {
  if (!data) return
  realtimeStatus.listeningStatus = data.listeningStatus || 'IDLE'
  realtimeStatus.streamStatus = data.streamStatus || 'NOT_CONNECTED'
  realtimeStatus.currentChannel = data.currentChannel ?? null
  realtimeStatus.currentTaskId = data.currentTaskId ?? null
  realtimeStatus.currentTranscript = data.currentTranscript || null
  realtimeStatus.recentSegments = Array.isArray(data.recentSegments) ? data.recentSegments : []
  realtimeStatus.providerInfo = data.providerInfo || {}
  realtimeStatus.note = data.note || data.message || ''
  realtimeStatus.latestRiskLevel = data.latestRiskLevel || ''
  realtimeStatus.latestEventType = data.latestEventType || ''
  realtimeStatus.latestAlarmId = data.latestAlarmId ?? null
  realtimeStatus.latestAlarmLevel = data.latestAlarmLevel || ''
  realtimeStatus.latestAlarmStatus = data.latestAlarmStatus || ''
  realtimeStatus.latestAlarmReason = data.latestAlarmReason || ''
  if (data.currentTaskId) {
    realtimeControl.taskId = data.currentTaskId
  }
}

async function loadOverview() {
  loading.value = true
  try {
    const [ov, chs] = await Promise.all([getMonitorCenterOverview(), getChannels()])
    overview.systemStatus = ov?.systemStatus || 'ONLINE'
    overview.todayTaskCount = ov?.todayTaskCount || 0
    overview.todayAlarmCount = ov?.todayAlarmCount || 0
    overview.todayHighRiskEventCount = ov?.todayHighRiskEventCount || 0
    overview.channelCount = ov?.channelCount || 0
    overview.recentAlarms = Array.isArray(ov?.recentAlarms) ? ov.recentAlarms : []
    channels.value = Array.isArray(chs) ? chs : []
    applyRealtimeStatus(ov?.realtimeStatus)
  } finally {
    loading.value = false
  }
}

async function pullRealtimeStatus() {
  const data = await getRealtimeStatus()
  const previousAlarmId = realtimeStatus.latestAlarmId
  const previousRiskLevel = realtimeStatus.latestRiskLevel
  applyRealtimeStatus(data)
  const riskUpgradedToHigh = String(realtimeStatus.latestRiskLevel || '').toUpperCase() === 'HIGH' && String(previousRiskLevel || '').toUpperCase() !== 'HIGH'
  const alarmChanged = realtimeStatus.latestAlarmId && realtimeStatus.latestAlarmId !== previousAlarmId
  if (riskUpgradedToHigh || alarmChanged) {
    await loadOverview()
  }
}

function onChunkSelected(e) {
  const files = e?.target?.files || []
  selectedChunkFile.value = files[0] || null
}

function formatDateTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value).replace('T', ' ').slice(0, 19)
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).format(date)
}

function formatMsRange(startTime, endTime) {
  if (startTime == null && endTime == null) return ''
  const toText = (ms) => {
    const total = Math.max(0, Math.floor(Number(ms || 0) / 1000))
    const minutes = String(Math.floor(total / 60)).padStart(2, '0')
    const seconds = String(total % 60).padStart(2, '0')
    return `${minutes}:${seconds}`
  }
  if (startTime != null && endTime != null) {
    return `${toText(startTime)} - ${toText(endTime)}`
  }
  return toText(startTime ?? endTime)
}

function formatSegmentTime(item) {
  const dateText = formatDateTime(item?.timestamp)
  const rangeText = formatMsRange(item?.startTime, item?.endTime)
  return rangeText ? `${dateText}\n${rangeText}` : dateText
}

function goTask(taskId) {
  if (!taskId) return
  router.push(`/audio-tasks/${taskId}`)
}

function goAlarm(alarmId) {
  if (!alarmId) return
  router.push({ path: '/alarms', query: { alarmId: String(alarmId) } })
}

function goHistory(taskId) {
  if (!taskId) return
  router.push({ path: '/history-records', query: { taskId: String(taskId) } })
}

function goAnalysis(taskId) {
  if (!taskId) return
  router.push({ path: '/analysis-reports', query: { taskId: String(taskId) } })
}

async function startListening() {
  starting.value = true
  try {
    const data = await startRealtime({ channelId: realtimeControl.channelId, mode: realtimeControl.mode })
    applyRealtimeStatus(data)
    ElMessage.success('实时监听已启动')
  } finally {
    starting.value = false
  }
}

async function stopListening() {
  stopping.value = true
  try {
    const data = await stopRealtime()
    applyRealtimeStatus(data)
    ElMessage.success('实时监听已停止')
  } finally {
    stopping.value = false
  }
}

async function sendChunk() {
  if (!selectedChunkFile.value) {
    ElMessage.warning('请先选择音频片段')
    return
  }
  sendingChunk.value = true
  try {
    const formData = new FormData()
    formData.append('audio', selectedChunkFile.value)
    if (realtimeControl.taskId) formData.append('taskId', String(realtimeControl.taskId))
    const result = await sendRealtimeChunk(formData)
    ElMessage.success(`识别完成：${(result?.transcriptText || '').slice(0, 60)}`)
    await pullRealtimeStatus()
  } finally {
    sendingChunk.value = false
  }
}

async function manualAlarm() {
  manualAlarmLoading.value = true
  try {
    await createManualAlarm({
      taskId: realtimeControl.taskId || undefined,
      channelId: realtimeStatus.currentChannel || realtimeControl.channelId,
      alarmLevel: 'HIGH',
      triggerReason: '值守员手动触发实时监听报警',
      remark: '监控中心手动报警按钮触发'
    })
    ElMessage.success('手动报警已创建')
    await loadOverview()
  } finally {
    manualAlarmLoading.value = false
  }
}

onMounted(async () => {
  await loadOverview()
  pollTimer = setInterval(() => {
    pullRealtimeStatus().catch(() => {})
  }, 3000)
  overviewTimer = setInterval(() => {
    loadOverview().catch(() => {})
  }, 10000)
})

onBeforeUnmount(() => {
  clearInterval(clockTimer)
  if (pollTimer) clearInterval(pollTimer)
  if (overviewTimer) clearInterval(overviewTimer)
})
</script>

<style scoped>
.monitor-page {
  background: #f6f8fc;
}

.page-header-bar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 18px;
  padding-bottom: 18px;
  border-bottom: 1px solid #e8edf5;
}

.page-eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #0ea5e9;
}

.page-title {
  margin: 4px 0;
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
}

.page-desc {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.alert-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #fecaca;
  background: linear-gradient(135deg, #fff1f2, #ffe4e6);
}

.alert-banner__title {
  font-size: 15px;
  font-weight: 800;
  color: #991b1b;
}

.alert-banner__text {
  margin-top: 4px;
  color: #7f1d1d;
  font-size: 13px;
}

.alert-banner__actions {
  display: flex;
  gap: 8px;
}

.table-scroll {
  overflow-x: auto;
}

.alarm-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.realtime-link-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin: 12px 0 14px;
}

.link-chip {
  padding: 6px 10px;
  border-radius: 999px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  font-size: 12px;
  color: #1d4ed8;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.metric-card {
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 12px;
  padding: 12px 14px;
}

.metric-label {
  font-size: 12px;
  color: #64748b;
}

.metric-value {
  margin-top: 6px;
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
}

.metric-value--small {
  font-size: 20px;
}

.metric-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.realtime-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.rt-card {
  background: #f8fafc;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  padding: 12px;
}

.rt-card--alarm {
  background: #fff1f2;
  border-color: #fecdd3;
}

.rt-label {
  font-size: 11px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.rt-value {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

.rt-time {
  font-size: 18px;
}

.rt-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.control-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.file-btn {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fff;
  color: #475569;
  font-size: 12px;
  cursor: pointer;
  max-width: 220px;
}

.file-btn span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-input {
  display: none;
}

.provider-note {
  margin-bottom: 12px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #eef6ff;
  color: #35516b;
  font-size: 12px;
  border: 1px solid #dbeafe;
}

.realtime-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.sub-panel {
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  padding: 10px;
}

.sub-panel-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8px;
}

.transcript-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  min-height: 80px;
  line-height: 1.6;
  color: #1e293b;
}

@media (max-width: 1100px) {
  .overview-grid,
  .realtime-metrics,
  .realtime-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 768px) {
  .overview-grid,
  .realtime-metrics,
  .realtime-grid {
    grid-template-columns: 1fr;
  }
}
</style>
