<template>
  <PageContainer class="crg-page dash-page" v-loading="loading">
    <div class="dash-header">
      <div class="dash-header-left">
        <div class="dash-eyebrow">COMMAND OVERVIEW</div>
        <h1 class="dash-title">值守总览</h1>
        <p class="dash-desc">实时观察音频任务执行态势与风险告警动态</p>
      </div>
      <div class="dash-header-right">
        <div class="live-indicator">
          <span class="live-dot"></span>
          <span class="live-text">实时监控中</span>
        </div>
        <div class="dash-time">{{ currentTime }}</div>
      </div>
    </div>

    <!-- 核心指标 -->
    <div class="metric-grid">
      <div class="metric-card metric-primary">
        <div class="metric-icon">
          <svg viewBox="0 0 24 24" fill="none"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2M9 5a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2M9 5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
        </div>
        <div class="metric-body">
          <div class="metric-value">{{ metrics.todayTasks }}</div>
          <div class="metric-label">今日任务</div>
          <div class="metric-hint">当日新建任务总数</div>
        </div>
        <div class="metric-bg-shape"></div>
      </div>

      <div class="metric-card metric-warning">
        <div class="metric-icon">
          <svg viewBox="0 0 24 24" fill="none"><path d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </div>
        <div class="metric-body">
          <div class="metric-value">{{ metrics.alarmCount }}</div>
          <div class="metric-label">告警总数</div>
          <div class="metric-hint">当前告警记录</div>
        </div>
        <div class="metric-bg-shape"></div>
      </div>

      <div class="metric-card metric-danger">
        <div class="metric-icon">
          <svg viewBox="0 0 24 24" fill="none"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </div>
        <div class="metric-body">
          <div class="metric-value">{{ metrics.highRiskCount }}</div>
          <div class="metric-label">高风险事件</div>
          <div class="metric-hint">riskLevel = HIGH</div>
        </div>
        <div class="metric-bg-shape"></div>
      </div>

      <div class="metric-card metric-success">
        <div class="metric-icon">
          <svg viewBox="0 0 24 24" fill="none"><path d="M8.56 2.9A7 7 0 0 1 19 9v4m-7-4a7 7 0 0 0-7 7v1h14v-1a7 7 0 0 0-.44-2.47M6 22h12M3 3l18 18" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </div>
        <div class="metric-body">
          <div class="metric-value">{{ metrics.channelCount }}</div>
          <div class="metric-label">频道数</div>
          <div class="metric-hint">已纳入值守频道</div>
        </div>
        <div class="metric-bg-shape"></div>
      </div>
    </div>

    <!-- 表格区 -->
    <div class="content-grid">
      <div class="data-panel">
        <div class="panel-head">
          <div class="panel-head-left">
            <span class="panel-accent"></span>
            <span class="panel-title">最近任务</span>
          </div>
          <span class="panel-badge">最新 6 条</span>
        </div>
        <div class="panel-body">
          <template v-if="recentTasks.length">
            <el-table :data="recentTasks" empty-text="暂无任务数据" class="dash-table">
              <el-table-column prop="id" label="任务ID" width="90" />
              <el-table-column prop="channelId" label="频道" width="80" />
              <el-table-column label="状态" width="140">
                <template #default="scope">
                  <StatusTag :status="scope.row.taskStatus" />
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" min-width="160" />
            </el-table>
          </template>
          <EmptyState v-else description="暂无任务数据" />
        </div>
      </div>

      <div class="data-panel">
        <div class="panel-head">
          <div class="panel-head-left">
            <span class="panel-accent panel-accent--warn"></span>
            <span class="panel-title">最近告警</span>
          </div>
          <span class="panel-badge">最新 6 条</span>
        </div>
        <div class="panel-body">
          <template v-if="recentAlarms.length">
            <el-table :data="recentAlarms" empty-text="暂无告警记录" class="dash-table">
              <el-table-column prop="taskId" label="任务ID" width="90" />
              <el-table-column label="等级" width="100">
                <template #default="scope">
                  <RiskTag :level="scope.row.alarmLevel" />
                </template>
              </el-table-column>
              <el-table-column prop="triggerSource" label="触发来源" width="110" />
              <el-table-column label="状态" width="120">
                <template #default="scope">
                  <StatusTag :status="scope.row.alarmStatus" />
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="创建时间" min-width="150" />
            </el-table>
          </template>
          <EmptyState v-else description="暂无告警记录" />
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getChannels } from '../api/channel'
import { getAudioTasks } from '../api/audioTask'
import { getAlarmList } from '../api/alarm'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import InfoCard from '../components/InfoCard.vue'
import SectionPanel from '../components/SectionPanel.vue'
import EmptyState from '../components/EmptyState.vue'
import RiskTag from '../components/RiskTag.vue'
import StatusTag from '../components/StatusTag.vue'

const loading = ref(false)
const channels = ref([])
const tasks = ref([])
const alarms = ref([])
const currentTime = ref(new Date().toLocaleString('zh-CN'))

const metrics = computed(() => {
  const today = new Date().toDateString()
  const todayTasks = tasks.value.filter((t) => new Date(t.createTime || '').toDateString() === today)
  const highRisk = tasks.value.filter((t) => String(t.riskLevel || '').toUpperCase() === 'HIGH')

  return {
    todayTasks: todayTasks.length,
    alarmCount: alarms.value.length,
    highRiskCount: highRisk.length,
    channelCount: channels.value.length
  }
})

const recentTasks = computed(() => [...tasks.value].slice(0, 6))
const recentAlarms = computed(() => [...alarms.value].slice(0, 6))

async function loadData() {
  loading.value = true
  try {
    const [channelRes, taskRes, alarmRes] = await Promise.all([getChannels(), getAudioTasks(), getAlarmList()])
    channels.value = Array.isArray(channelRes) ? channelRes : []
    tasks.value = Array.isArray(taskRes) ? taskRes : []
    alarms.value = Array.isArray(alarmRes) ? alarmRes : []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
  setInterval(() => {
    currentTime.value = new Date().toLocaleString('zh-CN')
  }, 1000)
})
</script>

<style scoped>
.dash-page {
  background: #f6f8fc;
  min-height: calc(100vh - 60px);
}

/* ===== 页头 ===== */
.dash-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 28px;
  padding-bottom: 24px;
  border-bottom: 1px solid #e8edf5;
}

.dash-eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 2.5px;
  color: #0ea5e9;
  margin-bottom: 8px;
}

.dash-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 800;
  color: #0f172a;
  letter-spacing: -0.5px;
}

.dash-desc {
  margin: 0;
  font-size: 14px;
  color: #64748b;
}

.dash-header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.live-indicator {
  display: flex;
  align-items: center;
  gap: 7px;
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.4);
  animation: live-pulse 2s ease-in-out infinite;
}

@keyframes live-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(34, 197, 94, 0); }
}

.live-text {
  font-size: 13px;
  color: #22c55e;
  font-weight: 600;
}

.dash-time {
  font-size: 13px;
  color: #94a3b8;
  font-variant-numeric: tabular-nums;
}

/* ===== 指标卡片 ===== */
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 28px;
}

.metric-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 22px 20px;
  border-radius: 14px;
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.metric-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(0,0,0,0.1);
}

.metric-primary {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #bfdbfe;
}
.metric-warning {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border: 1px solid #fde68a;
}
.metric-danger {
  background: linear-gradient(135deg, #fff1f2 0%, #ffe4e6 100%);
  border: 1px solid #fecdd3;
}
.metric-success {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border: 1px solid #bbf7d0;
}

.metric-icon {
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.metric-icon svg {
  width: 22px;
  height: 22px;
}

.metric-primary .metric-icon { background: rgba(59, 130, 246, 0.12); color: #2563eb; }
.metric-warning .metric-icon { background: rgba(245, 158, 11, 0.12); color: #d97706; }
.metric-danger  .metric-icon { background: rgba(239, 68, 68, 0.12);  color: #dc2626; }
.metric-success .metric-icon { background: rgba(34, 197, 94, 0.12);  color: #16a34a; }

.metric-body {
  min-width: 0;
}

.metric-value {
  font-size: 32px;
  font-weight: 800;
  line-height: 1;
  margin-bottom: 5px;
  font-variant-numeric: tabular-nums;
}

.metric-primary .metric-value { color: #1d4ed8; }
.metric-warning .metric-value { color: #b45309; }
.metric-danger  .metric-value { color: #b91c1c; }
.metric-success .metric-value { color: #15803d; }

.metric-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 2px;
}

.metric-hint {
  font-size: 12px;
  color: #6b7280;
}

.metric-bg-shape {
  position: absolute;
  right: -20px;
  bottom: -20px;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  opacity: 0.08;
}

.metric-primary .metric-bg-shape { background: #3b82f6; }
.metric-warning .metric-bg-shape { background: #f59e0b; }
.metric-danger  .metric-bg-shape { background: #ef4444; }
.metric-success .metric-bg-shape { background: #22c55e; }

/* ===== 数据面板 ===== */
.content-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.data-panel {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid #e8edf5;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04);
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 14px;
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
  background: #2563eb;
}

.panel-accent--warn {
  background: #f59e0b;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.panel-badge {
  font-size: 12px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 3px 10px;
  border-radius: 20px;
}

.panel-body {
  padding: 12px 14px 14px;
}

/* 表格样式 */
.dash-table {
  width: 100%;
}

:deep(.dash-table.el-table) {
  --el-table-border-color: #f1f5f9;
  --el-table-header-bg-color: #fafbfc;
  --el-table-header-text-color: #64748b;
  --el-table-row-hover-bg-color: #f8faff;
  font-size: 13px;
}

:deep(.dash-table.el-table th.el-table__cell) {
  font-weight: 600;
  font-size: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #e8edf5 !important;
}

:deep(.dash-table.el-table td.el-table__cell) {
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9 !important;
  color: #334155;
}

:deep(.dash-table.el-table::before) {
  display: none;
}

@media (max-width: 1100px) {
  .metric-grid { grid-template-columns: repeat(2, 1fr); }
  .content-grid { grid-template-columns: 1fr; }
  .dash-header { flex-direction: column; align-items: flex-start; gap: 12px; }
}

@media (max-width: 640px) {
  .metric-grid { grid-template-columns: 1fr; }
}
</style>