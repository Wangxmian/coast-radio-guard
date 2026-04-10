<template>
  <PageContainer class="crg-page alarm-page" v-loading="loading">
    <div class="page-header-bar">
      <div class="header-left">
        <div class="page-eyebrow">ALARM CENTER</div>
        <h1 class="page-title">告警中心</h1>
        <p class="page-desc">聚合任务告警记录，支持签收、处理、关闭与误报标记</p>
      </div>
      <div class="header-actions">
        <el-button @click="loadData" :icon="'Refresh'">刷新</el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-label">筛选条件</div>
      <div class="filter-controls">
        <el-select v-model="filters.alarmStatus" clearable placeholder="全部状态" style="width: 160px">
          <el-option label="未处理" value="UNHANDLED" />
          <el-option label="已签收" value="ACKNOWLEDGED" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已处理" value="RESOLVED" />
          <el-option label="已关闭" value="CLOSED" />
          <el-option label="误报" value="FALSE_ALARM" />
        </el-select>
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="搜索触发原因 / 备注"
          style="width: 240px"
          prefix-icon="Search"
        />
        <el-input-number v-model="filters.taskId" :min="1" :step="1" placeholder="任务ID" style="width: 120px" />
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>
      <div class="filter-stat" v-if="list.length">
        共 <strong>{{ list.length }}</strong> 条记录 &nbsp;|&nbsp;
        高风险 <strong class="stat-danger">{{ highCount }}</strong> 条
      </div>
    </div>

    <!-- 告警表格 -->
    <div class="data-panel">
      <div class="panel-head">
        <div class="panel-head-left">
          <span class="panel-accent"></span>
          <span class="panel-title">告警列表</span>
          <span class="panel-sub">高风险项自动高亮，支持状态流转</span>
        </div>
      </div>
      <div class="panel-body">
        <template v-if="list.length">
          <div class="table-scroll">
          <el-table :data="list" empty-text="暂无告警记录" :row-class-name="rowClassName" class="alarm-table">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="taskId" label="任务ID" width="90" />
            <el-table-column label="等级" width="105">
              <template #default="scope">
                <RiskTag :level="scope.row.alarmLevel" />
              </template>
            </el-table-column>
            <el-table-column prop="triggerSource" label="触发来源" width="120" />
            <el-table-column label="状态" width="125">
              <template #default="scope">
                <StatusTag :status="scope.row.alarmStatus" />
              </template>
            </el-table-column>
            <el-table-column prop="eventType" label="事件类型" width="150" show-overflow-tooltip />
            <el-table-column prop="triggerReason" label="触发原因" min-width="200" show-overflow-tooltip />
            <el-table-column prop="latestRemark" label="最新备注" min-width="160" show-overflow-tooltip />
            <el-table-column prop="createTime" label="创建时间" width="165" />
            <el-table-column label="操作" width="220">
              <template #default="scope">
                <div class="op-row">
                  <el-button size="small" text @click="goTask(scope.row.taskId)" :disabled="!scope.row.taskId">
                    查看任务
                  </el-button>
                  <el-button size="small" text @click="goAnalysis(scope.row.taskId)" :disabled="!scope.row.taskId">
                    查看分析
                  </el-button>
                  <el-button size="small" text type="primary" @click="showDetail(scope.row)">
                    详情
                  </el-button>
                  <el-dropdown trigger="click" @command="(cmd) => doAction(scope.row, cmd)">
                    <el-button size="small" type="primary">处理 <el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="ack">✔ 签收</el-dropdown-item>
                        <el-dropdown-item command="process">⏳ 处理中</el-dropdown-item>
                        <el-dropdown-item command="resolve">✅ 已处理</el-dropdown-item>
                        <el-dropdown-item command="close" divided>✖ 关闭</el-dropdown-item>
                        <el-dropdown-item command="false">⚐ 误报</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </template>
            </el-table-column>
          </el-table>
          </div>
        </template>
        <EmptyState v-else description="暂无告警记录" />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="告警详情" size="42%">
      <div v-if="detailData" class="drawer-content">
        <div class="detail-section">
          <div class="detail-section-title">基本信息</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="告警ID">{{ detailData.alarm?.id }}</el-descriptions-item>
            <el-descriptions-item label="任务ID">{{ detailData.alarm?.taskId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="分析ID">{{ detailData.alarm?.analysisId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="告警状态">{{ detailData.alarm?.alarmStatus || '-' }}</el-descriptions-item>
            <el-descriptions-item label="事件类型">{{ detailData.alarm?.eventType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="触发原因">{{ detailData.alarm?.triggerReason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="最新备注">{{ detailData.alarm?.latestRemark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <div class="detail-section-title">关联文本与分析</div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="转录文本">{{ detailData.asrResult?.correctedTranscript || detailData.asrResult?.transcriptText || detailData.alarm?.transcriptText || '-' }}</el-descriptions-item>
            <el-descriptions-item label="事件摘要">{{ detailData.llmAnalysisResult?.eventSummary || detailData.alarm?.eventSummary || '-' }}</el-descriptions-item>
            <el-descriptions-item label="分析原因">{{ detailData.llmAnalysisResult?.reason || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <div class="detail-section-title">审计日志</div>
          <el-timeline>
            <el-timeline-item
              v-for="item in detailData.auditLogs || []"
              :key="item.id"
              :timestamp="item.createTime"
              placement="top"
            >
              <div class="timeline-action">{{ item.actionType }}: {{ item.fromStatus || '-' }} → {{ item.toStatus || '-' }}</div>
              <div class="audit-note">{{ item.operatorUsername || 'SYSTEM' }} / {{ item.remark || '-' }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </el-drawer>
  </PageContainer>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import {
  ackAlarm,
  closeAlarm,
  falseAlarm,
  getAlarmDetail,
  getAlarmList,
  processAlarm,
  resolveAlarm
} from '../api/alarm'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import SectionPanel from '../components/SectionPanel.vue'
import EmptyState from '../components/EmptyState.vue'
import RiskTag from '../components/RiskTag.vue'
import StatusTag from '../components/StatusTag.vue'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const list = ref([])
const detailVisible = ref(false)
const detailData = ref(null)

const filters = reactive({
  alarmStatus: '',
  keyword: '',
  taskId: null
})

const highCount = computed(() => list.value.filter(r => String(r.alarmLevel || '').toUpperCase() === 'HIGH').length)

function rowClassName({ row }) {
  return String(row.alarmLevel || '').toUpperCase() === 'HIGH' ? 'row-alarm-high' : ''
}

async function loadData() {
  loading.value = true
  try {
    list.value = (await getAlarmList({
      taskId: filters.taskId || undefined,
      alarmStatus: filters.alarmStatus || undefined,
      keyword: filters.keyword || undefined,
      page: 1,
      pageSize: 200
    })) || []
  } finally {
    loading.value = false
  }
}

async function showDetail(row) {
  detailData.value = await getAlarmDetail(row.id)
  detailVisible.value = true
}

function goTask(taskId) {
  if (!taskId) return
  router.push(`/audio-tasks/${taskId}`)
}

function goAnalysis(taskId) {
  if (!taskId) return
  router.push({ path: '/analysis-reports', query: { taskId: String(taskId) } })
}

async function doAction(row, cmd) {
  const actionMap = {
    ack: { fn: ackAlarm, label: '签收' },
    process: { fn: processAlarm, label: '转处理中' },
    resolve: { fn: resolveAlarm, label: '标记已处理' },
    close: { fn: closeAlarm, label: '关闭' },
    false: { fn: falseAlarm, label: '标记误报' }
  }
  const target = actionMap[cmd]
  if (!target) return

  const { value } = await ElMessageBox.prompt(`请输入${target.label}备注（可为空）`, target.label, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPlaceholder: '处理说明'
  }).catch(() => ({ value: null }))

  if (value === null) return

  await target.fn(row.id, value)
  ElMessage.success('状态更新成功')
  await loadData()
  if (detailVisible.value && detailData.value?.alarm?.id === row.id) {
    detailData.value = await getAlarmDetail(row.id)
  }
}

function applyRouteQuery() {
  filters.taskId = route.query.taskId ? Number(route.query.taskId) : null
  filters.keyword = route.query.keyword ? String(route.query.keyword) : ''
  if (route.query.alarmStatus) {
    filters.alarmStatus = String(route.query.alarmStatus)
  }
}

watch(() => route.query, async () => {
  applyRouteQuery()
  await loadData()
  if (route.query.alarmId) {
    const row = list.value.find((item) => Number(item.id) === Number(route.query.alarmId))
    if (row) {
      await showDetail(row)
    }
  }
})

onMounted(async () => {
  applyRouteQuery()
  await loadData()
  if (route.query.alarmId) {
    const row = list.value.find((item) => Number(item.id) === Number(route.query.alarmId))
    if (row) {
      await showDetail(row)
    }
  }
})
</script>

<style scoped>
.alarm-page {
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
  color: #f59e0b;
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

/* ===== 筛选栏 ===== */
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
  flex: 1;
}

.filter-stat {
  margin-left: auto;
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
}

.stat-danger {
  color: #dc2626;
}

/* ===== 数据面板 ===== */
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
  justify-content: space-between;
  padding: 16px 20px 12px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-head-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-accent {
  display: block;
  width: 4px;
  height: 18px;
  border-radius: 2px;
  background: #f59e0b;
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

/* ===== 表格 ===== */
:deep(.alarm-table.el-table) {
  --el-table-border-color: #f1f5f9;
  --el-table-header-bg-color: #fafbfc;
  --el-table-header-text-color: #64748b;
  --el-table-row-hover-bg-color: #f8faff;
  font-size: 13px;
}

:deep(.alarm-table.el-table th.el-table__cell) {
  font-weight: 600;
  font-size: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #e8edf5 !important;
}

:deep(.alarm-table.el-table td.el-table__cell) {
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9 !important;
  color: #334155;
}

:deep(.alarm-table.el-table::before) {
  display: none;
}

/* 高风险行高亮 */
:deep(.row-alarm-high td) {
  background: linear-gradient(90deg, rgba(254, 226, 226, 0.6) 0%, rgba(255, 255, 255, 0) 60%) !important;
}

:deep(.row-alarm-high td:first-child) {
  border-left: 3px solid #ef4444;
}

/* 操作按钮行 */
.op-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

/* 抽屉内容 */
.drawer-content {
  padding: 4px 0;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section-title {
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

.timeline-action {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.audit-note {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
}
</style>
