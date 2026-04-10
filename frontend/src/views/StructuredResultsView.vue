<template>
  <PageContainer class="crg-page structured-page" v-loading="loading">
    <div class="page-header-bar">
      <div class="header-left">
        <div class="page-eyebrow">ANALYSIS REPORTS</div>
        <h1 class="page-title">分析报告</h1>
        <p class="page-desc">批量检索已完成分析的任务报告，单条详情请前往音频任务页查看。</p>
      </div>
      <div class="header-actions">
        <el-button @click="search">刷新</el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-label">筛选条件</div>
      <div class="filter-controls">
        <el-input-number v-model="filters.taskId" :min="1" :step="1" placeholder="任务ID" style="width: 120px" />
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="关键词（摘要/事件类型/转写）"
          style="width: 250px"
          prefix-icon="Search"
        />
        <el-select v-model="filters.riskLevel" clearable placeholder="风险等级" style="width: 140px">
          <el-option label="HIGH 高风险" value="HIGH" />
          <el-option label="MEDIUM 中风险" value="MEDIUM" />
          <el-option label="LOW 低风险" value="LOW" />
        </el-select>
        <el-button type="primary" :loading="searching" @click="search">查询</el-button>
      </div>
    </div>

    <!-- 结果区 -->
    <div class="result-grid">
      <div class="data-panel">
        <div class="panel-head">
          <span class="panel-accent"></span>
          <span class="panel-title">任务结果列表</span>
          <span class="panel-sub">点击行查看右侧详情</span>
        </div>
        <div class="panel-body">
          <template v-if="rows.length">
            <div class="table-scroll">
              <el-table
                :data="rows"
                empty-text="暂无结果"
                @row-click="pickRow"
                :row-class-name="rowClassName"
                highlight-current-row
                class="result-table"
              >
                <el-table-column prop="taskId" label="任务ID" width="90" />
                <el-table-column prop="channelId" label="频道" width="80" />
                <el-table-column label="风险等级" width="105">
                  <template #default="scope">
                    <RiskTag :level="scope.row.riskLevel" />
                  </template>
                </el-table-column>
                <el-table-column prop="eventType" label="事件类型" width="150" show-overflow-tooltip />
                <el-table-column prop="eventSummary" label="摘要" min-width="160" show-overflow-tooltip />
                <el-table-column label="告警状态" width="120">
                  <template #default="scope">
                    <span>{{ scope.row.alarmStatus || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="联动" width="180">
                  <template #default="scope">
                    <div class="action-group">
                      <el-button size="small" text type="primary" @click.stop="goTask(scope.row.taskId)">任务</el-button>
                      <el-button size="small" text @click.stop="goAlarm(scope.row.alarmId)" :disabled="!scope.row.alarmId">告警</el-button>
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
          <EmptyState v-else description="暂无结构化结果" />
        </div>
      </div>

      <div class="data-panel">
        <div class="panel-head">
          <span class="panel-accent panel-accent--purple"></span>
          <span class="panel-title">结构化详情</span>
          <span class="panel-sub">友好视图 / JSON 双视图</span>
        </div>
        <div class="panel-body">
          <JsonViewerPanel :json="selectedRaw">
            <template #friendly>
              <div v-if="selectedRaw && selectedRaw.task" class="friendly">
                <div class="friendly-head">
                  <div class="friendly-id">任务 #{{ selectedRaw.task.id }}</div>
                  <RiskTag :level="selectedRaw.llmAnalysisResult?.riskLevel" />
                </div>
                <div class="provider-line" v-if="providerSummary">{{ providerSummary }}</div>
                <KeyValueList :items="friendlyItems" />
                <TranscriptComparePanel
                  :raw-text="selectedRaw.asrResult?.rawTranscript || selectedRaw.asrResult?.transcriptText || ''"
                  :corrected-text="selectedRaw.asrResult?.correctedTranscript || selectedRaw.asrResult?.transcriptText || ''"
                  :correction-diff="selectedRaw.asrResult?.correctionDiff || ''"
                  :provider="selectedRaw.asrResult?.correctionProvider || ''"
                  :fallback="Boolean(selectedRaw.asrResult?.correctionFallback)"
                />
                <div class="copy-actions">
                  <el-button size="small" @click="copyText(selectedRaw.llmAnalysisResult?.eventSummary || '')">复制摘要</el-button>
                  <el-button size="small" @click="copyText(selectedRaw.asrResult?.correctedTranscript || selectedRaw.asrResult?.transcriptText || '')">复制转写</el-button>
                  <el-button size="small" @click="goTask(selectedRaw.task?.id)">任务详情</el-button>
                  <el-button size="small" :disabled="!(selectedRaw.alarmRecords || []).length" @click="goAlarm(selectedRaw.alarmRecords?.[0]?.id)">关联告警</el-button>
                </div>
              </div>
              <EmptyState v-else description="请选择左侧任务查看详情" />
            </template>
          </JsonViewerPanel>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStructuredResultDetail, getStructuredResultJson, getStructuredResults } from '../api/structured'
import PageContainer from '../components/PageContainer.vue'
import EmptyState from '../components/EmptyState.vue'
import JsonViewerPanel from '../components/JsonViewerPanel.vue'
import KeyValueList from '../components/KeyValueList.vue'
import RiskTag from '../components/RiskTag.vue'
import TranscriptComparePanel from '../components/TranscriptComparePanel.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const searching = ref(false)
const rows = ref([])
const selectedRaw = ref(null)

const filters = reactive({
  taskId: null,
  keyword: '',
  riskLevel: ''
})

const friendlyItems = computed(() => {
  const obj = selectedRaw.value || {}
  return [
    { key: 'event-type', label: '事件类型', value: obj.llmAnalysisResult?.eventType || '-' },
    { key: 'summary', label: '事件摘要', value: obj.llmAnalysisResult?.eventSummary || '-' },
    { key: 'reason', label: '分析原因', value: obj.llmAnalysisResult?.reason || '-' },
    { key: 'analysis-transcript', label: '分析文本', value: obj.analysisTranscript || obj.asrResult?.correctedTranscript || obj.asrResult?.transcriptText || '-' },
    { key: 'alarm', label: '告警数', value: Array.isArray(obj.alarmRecords) ? obj.alarmRecords.length : 0 }
  ]
})

const providerSummary = computed(() => {
  const p = selectedRaw.value?.providerInfo || {}
  const s = selectedRaw.value?.stageInfo || {}
  const f = selectedRaw.value?.fallbackInfo || {}
  const stages = ['vad', 'se', 'asr', 'llm']
  const parts = stages
    .map((stage) => {
      const provider = p[stage] || s[stage]?.effectiveProvider
      if (!provider) return null
      const fallback = Boolean(s[stage]?.fallbackUsed) || Boolean(f[stage]?.fallbackUsed)
      return `${stage.toUpperCase()}:${provider}${fallback ? '(fallback)' : ''}`
    })
    .filter(Boolean)
  return parts.length ? parts.join(' | ') : ''
})

function goTask(taskId) {
  if (!taskId) return
  router.push(`/audio-tasks/${taskId}`)
}

function goAlarm(alarmId) {
  if (!alarmId) return
  router.push({ path: '/alarms', query: { alarmId: String(alarmId) } })
}

function rowClassName({ row }) {
  return String(row.riskLevel || '').toUpperCase() === 'HIGH' ? 'row-high' : ''
}

async function search() {
  searching.value = true
  try {
    const data = await getStructuredResults({
      page: 1,
      pageSize: 100,
      taskId: filters.taskId || undefined,
      keyword: filters.keyword || undefined,
      riskLevel: filters.riskLevel || undefined
    })

    rows.value = Array.isArray(data?.records) ? data.records : []

    if (rows.value.length) {
      await pickRow(rows.value[0])
    } else {
      selectedRaw.value = null
    }
  } finally {
    searching.value = false
  }
}

async function pickRow(row) {
  if (!row?.taskId) return
  loading.value = true
  try {
    try {
      selectedRaw.value = await getStructuredResultJson(row.taskId)
    } catch (_e) {
      selectedRaw.value = await getStructuredResultDetail(row.taskId)
    }
  } finally {
    loading.value = false
  }
}

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text || '')
    ElMessage.success('复制成功')
  } catch (_e) {
    ElMessage.error('复制失败')
  }
}

function applyRouteQuery() {
  filters.taskId = route.query.taskId ? Number(route.query.taskId) : null
  filters.keyword = route.query.keyword ? String(route.query.keyword) : ''
  filters.riskLevel = route.query.riskLevel ? String(route.query.riskLevel) : ''
}

watch(() => route.query, () => {
  applyRouteQuery()
  search()
})

onMounted(() => {
  applyRouteQuery()
  search()
})
</script>

<style scoped>
.structured-page {
  background: #f6f8fc;
}

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

.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  padding: 14px 18px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e8edf5;
  margin-bottom: 20px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.03);
}

.filter-label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  white-space: nowrap;
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.data-panel {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e8edf5;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
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

.panel-accent--purple {
  background: #8b5cf6;
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
  padding: 10px 14px 14px;
}

.table-scroll {
  overflow-x: auto;
}

/* 表格 */
:deep(.result-table.el-table) {
  --el-table-border-color: #f1f5f9;
  --el-table-header-bg-color: #fafbfc;
  --el-table-header-text-color: #64748b;
  --el-table-row-hover-bg-color: #f0f9ff;
  --el-table-current-row-bg-color: #e0f2fe;
  font-size: 13px;
  cursor: pointer;
}

:deep(.result-table.el-table th.el-table__cell) {
  font-weight: 600;
  font-size: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #e8edf5 !important;
}

:deep(.result-table.el-table td.el-table__cell) {
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9 !important;
  color: #334155;
}

:deep(.result-table.el-table::before) { display: none; }

:deep(.row-high td) {
  background: rgba(254, 226, 226, 0.3) !important;
}

/* 友好视图 */
.friendly {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.friendly-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #f1f5f9;
}

.friendly-id {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.provider-line {
  margin-top: -2px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #eef6ff;
  border: 1px solid #dbeafe;
  font-size: 12px;
  color: #35516b;
}

.copy-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

@media (max-width: 1100px) {
  .result-grid {
    grid-template-columns: 1fr;
  }
}
</style>
