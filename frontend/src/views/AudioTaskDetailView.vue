<template>
  <PageContainer class="crg-page detail-page" v-loading="loading">
    <div class="page-header-bar">
      <div class="header-left">
        <div class="page-eyebrow">TASK ANALYSIS</div>
        <h1 class="page-title">任务分析详情</h1>
        <p class="page-desc">查看任务执行链路、语音识别结果与风险告警分析</p>
      </div>
      <div class="header-actions">
        <el-button @click="router.back()">← 返回</el-button>
        <el-button :loading="refreshing" @click="refreshDetail">刷新结果</el-button>
      </div>
    </div>

    <!-- Hero 卡片 -->
    <div class="hero-card" :class="heroClass">
      <div class="hero-left">
        <div class="hero-id">任务 #{{ detail.task?.id || route.params.id }}</div>
        <div class="hero-meta">
          <span class="meta-item">
            <svg viewBox="0 0 16 16" fill="none" width="13" height="13"><path d="M8 1.5A6.5 6.5 0 1 1 1.5 8 6.508 6.508 0 0 1 8 1.5z" stroke="currentColor" stroke-width="1.2"/><path d="M8 4.5v4l2.5 1.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>
            {{ detail.task?.createTime || '-' }}
          </span>
          <span class="meta-sep">|</span>
          <span class="meta-item">
            <svg viewBox="0 0 16 16" fill="none" width="13" height="13"><path d="M2 12.5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v.5H2v-.5z" stroke="currentColor" stroke-width="1.2"/><circle cx="8" cy="6" r="3" stroke="currentColor" stroke-width="1.2"/></svg>
            频道 {{ detail.task?.channelId ?? '-' }}
          </span>
        </div>
      </div>
      <div class="hero-right">
        <StatusTag :status="detail.task?.taskStatus" />
        <RiskTag :level="detail.llmAnalysisResult?.riskLevel || detail.task?.riskLevel" />
      </div>
    </div>

    <div class="nav-links">
      <el-button size="small" @click="goHistory(detail.task?.id)">历史记录</el-button>
      <el-button size="small" @click="goAnalysis(detail.task?.id)">分析报告</el-button>
      <el-button size="small" :disabled="!detail.alarmRecords.length" @click="goAlarm(detail.alarmRecords[0]?.id)">关联告警</el-button>
    </div>

    <div class="provider-strip" v-if="hasProviderInfo">
      <div class="provider-title">Provider 状态</div>
      <div class="provider-tags">
        <el-tag v-for="item in providerTagItems" :key="item.stage" :type="item.type" size="small">
          {{ item.stage.toUpperCase() }}: {{ item.provider }}
          <template v-if="item.fallback"> (fallback)</template>
        </el-tag>
      </div>
    </div>

    <!-- 基础信息 & 音频 -->
    <div class="info-grid">
      <div class="info-section">
        <div class="section-title">任务基本信息</div>
        <KeyValueList :items="taskItems" />
      </div>
      <div class="info-section">
        <div class="section-title">音频信息</div>
        <KeyValueList :items="audioItems" />
      </div>
    </div>

    <!-- ASR & LLM -->
    <div class="info-grid">
      <div class="info-section">
        <div class="section-title">ASR 识别结果 <span class="section-sub">自动语音识别转写</span></div>
        <KeyValueList :items="asrItems" />
        <TranscriptComparePanel
          class="transcript-compare"
          :raw-text="detail.asrResult?.rawTranscript || detail.asrResult?.transcriptText || ''"
          :corrected-text="detail.asrResult?.correctedTranscript || detail.asrResult?.transcriptText || ''"
          :correction-diff="detail.asrResult?.correctionDiff || ''"
          :provider="detail.asrResult?.correctionProvider || ''"
          :fallback="Boolean(detail.asrResult?.correctionFallback)"
        />
      </div>
      <div class="info-section">
        <div class="section-title">LLM 分析结果 <span class="section-sub">风险语义理解与事件提取</span></div>
        <div class="llm-panel">
          <div class="llm-row-inline">
            <div class="llm-field-row">
              <span class="llm-label">风险等级</span>
              <RiskTag :level="detail.llmAnalysisResult?.riskLevel" />
            </div>
            <div class="llm-field-row">
              <span class="llm-label">事件类型</span>
              <span class="llm-value">{{ detail.llmAnalysisResult?.eventType || '-' }}</span>
            </div>
          </div>
          <div class="llm-block">
            <p class="llm-block-label">事件摘要</p>
            <p class="llm-block-text">{{ detail.llmAnalysisResult?.eventSummary || '-' }}</p>
          </div>
          <div class="llm-block">
            <p class="llm-block-label">分析原因</p>
            <p class="llm-block-text">{{ detail.llmAnalysisResult?.reason || '-' }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 实体信息 & 告警 -->
    <div class="analysis-grid">
      <div class="data-panel">
        <div class="panel-head">
          <span class="panel-accent"></span>
          <span class="panel-title">实体信息</span>
          <span class="panel-sub">关键实体抽取结果</span>
        </div>
        <div class="panel-body">
          <template v-if="detail.entityResults.length">
            <el-table :data="detail.entityResults" empty-text="暂无实体信息" class="entity-table">
              <el-table-column prop="entityType" label="实体类型" min-width="130" />
              <el-table-column prop="entityValue" label="实体值" min-width="180" />
              <el-table-column prop="confidence" label="置信度" width="110" />
            </el-table>
          </template>
          <EmptyState v-else description="暂无实体信息" />
        </div>
      </div>

      <div class="data-panel">
        <div class="panel-head">
          <span class="panel-accent panel-accent--danger"></span>
          <span class="panel-title">风险事件与告警</span>
          <span class="panel-sub">规则与模型联合触发</span>
        </div>
        <div class="panel-body">
          <div class="risk-summary" v-if="detail.riskEvent || detail.llmAnalysisResult">
            <div class="risk-row">
              <span class="risk-key">风险等级</span>
              <RiskTag :level="detail.riskEvent?.riskLevel || detail.llmAnalysisResult?.riskLevel" />
            </div>
            <div class="risk-row" v-if="detail.riskEvent?.summary">
              <span class="risk-key">事件摘要</span>
              <span class="risk-val">{{ detail.riskEvent.summary }}</span>
            </div>
          </div>
          <template v-if="detail.alarmRecords.length">
            <el-table :data="detail.alarmRecords" empty-text="暂无告警数据" class="entity-table">
              <el-table-column label="等级" width="105">
                <template #default="scope">
                  <RiskTag :level="scope.row.alarmLevel" />
                </template>
              </el-table-column>
              <el-table-column prop="triggerSource" label="来源" width="110" />
              <el-table-column prop="triggerReason" label="触发原因" min-width="180" show-overflow-tooltip />
              <el-table-column label="状态" width="115">
                <template #default="scope">
                  <StatusTag :status="scope.row.alarmStatus" />
                </template>
              </el-table-column>
            </el-table>
          </template>
          <EmptyState v-else description="暂无告警数据" />
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAudioTaskAnalysis } from '../api/analysis'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import SectionPanel from '../components/SectionPanel.vue'
import KeyValueList from '../components/KeyValueList.vue'
import EmptyState from '../components/EmptyState.vue'
import RiskTag from '../components/RiskTag.vue'
import StatusTag from '../components/StatusTag.vue'
import TranscriptComparePanel from '../components/TranscriptComparePanel.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const refreshing = ref(false)

const detail = reactive({
  task: null,
  asrResult: null,
  llmAnalysisResult: null,
  entityResults: [],
  riskEvent: null,
  alarmRecords: [],
  providerInfo: {},
  stageInfo: {},
  fallbackInfo: {},
  analysisTranscript: '',
  analysisUsesCorrectedTranscript: false
})

const providerTagItems = computed(() => {
  const providerInfo = detail.providerInfo || {}
  const stageInfo = detail.stageInfo || {}
  const fallbackInfo = detail.fallbackInfo || {}
  const stages = ['vad', 'se', 'asr', 'llm']
  return stages
    .filter((stage) => providerInfo[stage] || stageInfo[stage]?.effectiveProvider)
    .map((stage) => {
      const provider = providerInfo[stage] || stageInfo[stage]?.effectiveProvider || '-'
      const fallback =
        Boolean(stageInfo[stage]?.fallbackUsed) ||
        Boolean(fallbackInfo[stage]?.fallbackUsed)
      const isMock = String(provider).toLowerCase().includes('mock')
      return {
        stage,
        provider,
        fallback,
        type: fallback || isMock ? 'warning' : 'success'
      }
    })
})

const hasProviderInfo = computed(() => providerTagItems.value.length > 0)

const heroClass = computed(() => {
  const level = String(detail.llmAnalysisResult?.riskLevel || detail.task?.riskLevel || '').toUpperCase()
  if (level === 'HIGH') return 'hero-high'
  if (level === 'MEDIUM') return 'hero-medium'
  return ''
})

const taskItems = computed(() => [
  { key: 'task-id', label: '任务ID', value: detail.task?.id || '-' },
  { key: 'channel-id', label: '频道ID', value: detail.task?.channelId || '-' },
  { key: 'task-type', label: '任务类型', value: detail.task?.taskType || '-' },
  { key: 'task-status', label: '任务状态', value: detail.task?.taskStatus || '-' },
  { key: 'created-at', label: '创建时间', value: detail.task?.createTime || '-' }
])

const audioItems = computed(() => [
  { key: 'pipeline', label: '处理链路', value: detail.task?.taskType === 'REALTIME' ? '实时 ASR -> LLM（无离线 SE 增强）' : '原始音频 -> SE 增强 -> ASR -> LLM' },
  { key: 'origin', label: '原始路径', value: detail.task?.originalFilePath || '-' },
  { key: 'enhanced', label: '增强路径', value: detail.task?.enhancedFilePath || '-' },
  { key: 'asr-input', label: 'ASR 输入', value: detail.task?.enhancedFilePath || detail.task?.originalFilePath || '-' }
])

const asrItems = computed(() => [
  { key: 'analysis-text', label: '分析使用文本', value: detail.analysisTranscript || detail.asrResult?.correctedTranscript || detail.asrResult?.transcriptText || '-' },
  { key: 'confidence', label: '识别置信度', value: detail.asrResult?.confidence ?? '-' },
  { key: 'provider', label: 'ASR Provider', value: detail.asrResult?.provider || '-' },
  { key: 'source-type', label: '来源类型', value: detail.asrResult?.sourceType || detail.task?.taskType || '-' },
  { key: 'correction-provider', label: '纠错 Provider', value: detail.asrResult?.correctionProvider || '-' },
  { key: 'correction-fallback', label: '纠错回退', value: detail.asrResult?.correctionFallback ? '是' : '否' }
])

function goHistory(taskId) {
  if (!taskId) return
  router.push({ path: '/history-records', query: { taskId: String(taskId) } })
}

function goAnalysis(taskId) {
  if (!taskId) return
  router.push({ path: '/analysis-reports', query: { taskId: String(taskId) } })
}

function goAlarm(alarmId) {
  if (!alarmId) return
  router.push({ path: '/alarms', query: { alarmId: String(alarmId) } })
}

async function loadDetail() {
  loading.value = true
  try {
    const data = await getAudioTaskAnalysis(route.params.id)
    detail.task = data?.task || null
    detail.asrResult = data?.asrResult || null
    detail.llmAnalysisResult = data?.llmAnalysisResult || null
    detail.entityResults = data?.entityResults || []
    detail.riskEvent = data?.riskEvent || null
    detail.alarmRecords = data?.alarmRecords || []
    detail.providerInfo = data?.providerInfo || {}
    detail.stageInfo = data?.stageInfo || {}
    detail.fallbackInfo = data?.fallbackInfo || {}
    detail.analysisTranscript = data?.analysisTranscript || ''
    detail.analysisUsesCorrectedTranscript = Boolean(data?.analysisUsesCorrectedTranscript)
  } finally {
    loading.value = false
  }
}

async function refreshDetail() {
  refreshing.value = true
  try {
    await loadDetail()
  } finally {
    refreshing.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.detail-page {
  background: #f6f8fc;
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
  color: #0ea5e9;
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

.header-actions {
  display: flex;
  gap: 10px;
}

.provider-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.provider-title {
  font-size: 12px;
  color: #475569;
  font-weight: 600;
  white-space: nowrap;
}

.provider-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.nav-links {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

/* ===== Hero 卡片 ===== */
.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-radius: 14px;
  background: linear-gradient(120deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #bae6fd;
  margin-bottom: 20px;
  transition: all 0.3s;
}

.hero-card.hero-high {
  background: linear-gradient(120deg, #fff1f2 0%, #ffe4e6 100%);
  border-color: #fecdd3;
}

.hero-card.hero-medium {
  background: linear-gradient(120deg, #fffbeb 0%, #fef3c7 100%);
  border-color: #fde68a;
}

.hero-id {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  margin-bottom: 10px;
}

.hero-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

.meta-sep {
  color: #cbd5e1;
}

.hero-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* ===== 信息区 ===== */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.info-section {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e8edf5;
  padding: 18px 20px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.04);
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-sub {
  font-size: 12px;
  font-weight: 400;
  color: #94a3b8;
}

.transcript-compare {
  margin-top: 14px;
}

/* LLM 面板 */
.llm-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.llm-row-inline {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.llm-field-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #f1f5f9;
}

.llm-label {
  font-size: 12px;
  color: #94a3b8;
  white-space: nowrap;
}

.llm-value {
  font-size: 13px;
  color: #334155;
  font-weight: 500;
}

.llm-block {
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #f1f5f9;
}

.llm-block-label {
  margin: 0 0 6px;
  font-size: 12px;
  color: #94a3b8;
  font-weight: 600;
}

.llm-block-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #334155;
}

/* ===== 分析区 ===== */
.analysis-grid {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 16px;
}

.data-panel {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e8edf5;
  overflow: hidden;
  box-shadow: 0 1px 6px rgba(0,0,0,0.04);
}

.panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px 12px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-accent {
  display: block;
  width: 4px;
  height: 18px;
  border-radius: 2px;
  background: #0ea5e9;
  flex-shrink: 0;
}

.panel-accent--danger {
  background: #ef4444;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.panel-sub {
  font-size: 13px;
  color: #94a3b8;
}

.panel-body {
  padding: 12px 14px 14px;
}

/* 风险摘要 */
.risk-summary {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  background: #fff8f8;
  border: 1px solid #fee2e2;
  border-radius: 10px;
  margin-bottom: 12px;
}

.risk-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.risk-key {
  font-size: 12px;
  color: #94a3b8;
  min-width: 56px;
}

.risk-val {
  font-size: 13px;
  color: #334155;
}

/* 表格 */
:deep(.entity-table.el-table) {
  --el-table-border-color: #f1f5f9;
  --el-table-header-bg-color: #fafbfc;
  --el-table-header-text-color: #64748b;
  --el-table-row-hover-bg-color: #f8faff;
  font-size: 13px;
}

:deep(.entity-table.el-table th.el-table__cell) {
  font-weight: 600;
  font-size: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #e8edf5 !important;
}

:deep(.entity-table.el-table td.el-table__cell) {
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9 !important;
  color: #334155;
}

:deep(.entity-table.el-table::before) {
  display: none;
}

@media (max-width: 1080px) {
  .info-grid, .analysis-grid {
    grid-template-columns: 1fr;
  }
  .hero-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .llm-row-inline {
    grid-template-columns: 1fr;
  }
}
</style>
