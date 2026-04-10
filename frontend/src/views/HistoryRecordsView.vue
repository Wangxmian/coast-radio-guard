<template>
  <PageContainer class="crg-page history-page" v-loading="loading">
    <div class="page-header-bar">
      <div class="header-left">
        <div class="page-eyebrow">HISTORY RECORDS</div>
        <h1 class="page-title">历史记录</h1>
        <p class="page-desc">按时间范围与关键词检索历史任务记录，支持导出与详情跳转</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadData">刷新</el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-label">筛选条件</div>
      <div class="filter-controls">
        <el-date-picker
          v-model="filters.range"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
        />
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="关键词（转写/事件/告警）"
          style="width: 220px"
          prefix-icon="Search"
        />
        <el-input-number v-model="filters.taskId" :min="1" :step="1" placeholder="任务ID" style="width: 120px" />
        <el-select v-model="filters.riskLevel" clearable placeholder="风险等级" style="width: 130px">
          <el-option label="HIGH 高风险" value="HIGH" />
          <el-option label="MEDIUM 中风险" value="MEDIUM" />
          <el-option label="LOW 低风险" value="LOW" />
        </el-select>
        <el-button type="primary" @click="loadData">筛选</el-button>
      </div>
    </div>

    <!-- 数据面板 -->
    <div class="data-panel">
      <div class="panel-head">
        <div class="panel-head-left">
          <span class="panel-accent"></span>
          <span class="panel-title">记录列表</span>
          <span class="panel-sub">来自 /api/history-records</span>
        </div>
        <div class="panel-actions">
          <el-button size="small" :loading="exporting" @click="exportCsv">📤 日志导出</el-button>
          <el-button size="small" disabled>☁ 云端存储（待接入）</el-button>
        </div>
      </div>
      <div class="panel-body">
        <template v-if="list.length">
          <el-table :data="list" empty-text="暂无记录" class="history-table">
            <el-table-column prop="taskId" label="任务ID" width="90" />
            <el-table-column prop="channelId" label="频道ID" width="90" />
            <el-table-column label="状态" width="125">
              <template #default="scope">
                <StatusTag :status="scope.row.taskStatus" />
              </template>
            </el-table-column>
            <el-table-column label="风险等级" width="110">
              <template #default="scope">
                <RiskTag :level="scope.row.riskLevel" />
              </template>
            </el-table-column>
            <el-table-column prop="eventType" label="事件类型" width="150" show-overflow-tooltip />
            <el-table-column label="转写对照" min-width="320">
              <template #default="scope">
                <div class="transcript-cell">
                  <div class="transcript-line">
                    <span class="transcript-badge">原始</span>
                    <span>{{ scope.row.rawTranscript || scope.row.transcriptText || '-' }}</span>
                  </div>
                  <div class="transcript-line transcript-line--corrected">
                    <span class="transcript-badge transcript-badge--green">纠错</span>
                    <span>{{ scope.row.correctedTranscript || scope.row.transcriptText || '-' }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="告警" width="110">
              <template #default="scope">
                <StatusTag v-if="scope.row.hasAlarm" :status="scope.row.alarmStatus" />
                <span v-else class="muted-cell">未触发</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="165" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="scope">
                <el-button size="small" text type="primary" @click="goDetail(scope.row.taskId)">任务</el-button>
                <el-button size="small" text @click="goAnalysis(scope.row.taskId)">分析</el-button>
                <el-button size="small" text :disabled="!scope.row.alarmId" @click="goAlarm(scope.row.alarmId)">告警</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>
        <EmptyState v-else description="暂无符合条件的历史记录" />
      </div>
    </div>
  </PageContainer>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { exportHistoryRecords, getHistoryRecords } from '../api/history'
import PageContainer from '../components/PageContainer.vue'
import EmptyState from '../components/EmptyState.vue'
import StatusTag from '../components/StatusTag.vue'
import RiskTag from '../components/RiskTag.vue'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const exporting = ref(false)
const list = ref([])

const filters = reactive({
  range: [],
  keyword: '',
  riskLevel: '',
  taskId: null
})

function goDetail(id) {
  router.push('/audio-tasks/' + id)
}

function goAnalysis(taskId) {
  router.push({ path: '/analysis-reports', query: { taskId: String(taskId) } })
}

function goAlarm(alarmId) {
  router.push({ path: '/alarms', query: { alarmId: String(alarmId) } })
}

function buildParams() {
  const params = {
    page: 1,
    pageSize: 200,
    taskId: filters.taskId || undefined,
    keyword: filters.keyword || undefined,
    riskLevel: filters.riskLevel || undefined
  }

  if (Array.isArray(filters.range) && filters.range.length === 2) {
    params.startTime = filters.range[0] + ' 00:00:00'
    params.endTime = filters.range[1] + ' 23:59:59'
  }
  return params
}

async function loadData() {
  loading.value = true
  try {
    const data = await getHistoryRecords(buildParams())
    list.value = Array.isArray(data?.records) ? data.records : []
  } finally {
    loading.value = false
  }
}

function applyRouteQuery() {
  filters.taskId = route.query.taskId ? Number(route.query.taskId) : null
  filters.keyword = route.query.keyword ? String(route.query.keyword) : ''
  filters.riskLevel = route.query.riskLevel ? String(route.query.riskLevel) : ''
}

async function exportCsv() {
  exporting.value = true
  try {
    const blob = await exportHistoryRecords(buildParams())
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'history_records_' + Date.now() + '.csv'
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

watch(() => route.query, () => {
  applyRouteQuery()
  loadData()
})

onMounted(() => {
  applyRouteQuery()
  loadData()
})
</script>

<style scoped>
.history-page {
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

.header-actions {
  display: flex;
  gap: 10px;
}

/* 筛选栏 */
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

/* 数据面板 */
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
  flex-wrap: wrap;
}

.panel-head-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.panel-accent {
  display: block;
  width: 4px;
  height: 18px;
  border-radius: 2px;
  background: #8b5cf6;
  flex-shrink: 0;
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

.panel-actions {
  display: flex;
  gap: 8px;
}

.panel-body {
  padding: 10px 14px 14px;
}

:deep(.history-table.el-table) {
  --el-table-border-color: #f1f5f9;
  --el-table-header-bg-color: #fafbfc;
  --el-table-header-text-color: #64748b;
  --el-table-row-hover-bg-color: #f8faff;
  font-size: 13px;
}

:deep(.history-table.el-table th.el-table__cell) {
  font-weight: 600;
  font-size: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #e8edf5 !important;
}

:deep(.history-table.el-table td.el-table__cell) {
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9 !important;
  color: #334155;
}

:deep(.history-table.el-table::before) {
  display: none;
}

.transcript-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  line-height: 1.6;
}

.transcript-line {
  color: #334155;
}

.transcript-line--corrected {
  color: #166534;
}

.transcript-badge {
  display: inline-block;
  min-width: 36px;
  margin-right: 8px;
  padding: 0 6px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #475569;
  font-size: 11px;
  font-weight: 700;
}

.transcript-badge--green {
  background: #dcfce7;
  color: #166534;
}

.muted-cell {
  color: #94a3b8;
}
</style>
