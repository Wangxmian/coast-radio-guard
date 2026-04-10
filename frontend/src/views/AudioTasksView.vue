<template>
  <PageContainer class="crg-page tasks-page" v-loading="loading">
    <div class="page-header-bar">
      <div class="header-left">
        <div class="page-eyebrow">AUDIO TASKS</div>
        <h1 class="page-title">音频任务</h1>
        <p class="page-desc">离线任务处理模式：上传/填写音频路径后触发 AI 链路并查看分析结果</p>
      </div>
      <div class="header-actions">
        <el-button @click="fetchList">刷新</el-button>
        <el-button type="primary" @click="createDialogVisible = true">+ 创建任务</el-button>
      </div>
    </div>

    <!-- 数据面板 -->
    <div class="data-panel">
      <div class="panel-head">
        <div class="panel-head-left">
          <span class="panel-accent"></span>
          <span class="panel-title">任务列表</span>
          <span class="panel-sub">支持执行、查看详情与删除</span>
        </div>
        <div class="pipeline-legend">
          <span class="legend-label">AI 处理链路：</span>
          <span class="pipeline-step step-se">SE</span>
          <span class="arrow">→</span>
          <span class="pipeline-step step-asr">ASR</span>
          <span class="arrow">→</span>
          <span class="pipeline-step step-llm">LLM</span>
        </div>
      </div>
      <div class="panel-body">
        <template v-if="list.length">
          <div class="table-scroll">
          <el-table :data="list" empty-text="暂无任务数据" :row-class-name="rowClassName" class="tasks-table">
            <el-table-column prop="id" label="任务ID" width="90" />
            <el-table-column prop="channelId" label="频道" width="80" />
            <el-table-column label="任务状态" width="125">
              <template #default="scope">
                <StatusTag :status="scope.row.taskStatus" />
              </template>
            </el-table-column>
            <el-table-column label="AI 处理链路" min-width="260">
              <template #default="scope">
                <div class="pipeline-row">
                  <div class="pipeline-badge" :class="getPipelineClass(scope.row.seStatus)">
                    <span class="badge-step">SE</span>
                    <span class="badge-status">{{ formatStatus(scope.row.seStatus) }}</span>
                  </div>
                  <span class="pipe-arrow">→</span>
                  <div class="pipeline-badge" :class="getPipelineClass(scope.row.asrStatus)">
                    <span class="badge-step">ASR</span>
                    <span class="badge-status">{{ formatStatus(scope.row.asrStatus) }}</span>
                  </div>
                  <span class="pipe-arrow">→</span>
                  <div class="pipeline-badge" :class="getPipelineClass(scope.row.llmStatus)">
                    <span class="badge-step">LLM</span>
                    <span class="badge-status">{{ formatStatus(scope.row.llmStatus) }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="风险等级" width="110">
              <template #default="scope">
                <RiskTag :level="scope.row.riskLevel" />
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="165" />
            <el-table-column label="操作" width="240">
              <template #default="scope">
                <div class="action-group">
                  <el-button size="small" text type="primary" @click="goDetail(scope.row.id)">详情</el-button>
                  <el-button
                    size="small"
                    type="success"
                    :loading="executingId === scope.row.id"
                    :disabled="executingId === scope.row.id || !canExecute(scope.row)"
                    @click="onExecute(scope.row)"
                  >
                    {{ canExecute(scope.row) ? '执行' : '实时任务' }}
                  </el-button>
                  <el-button size="small" type="danger" plain :loading="deletingId === scope.row.id" @click="onDelete(scope.row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          </div>
        </template>
        <EmptyState v-else description="暂无任务数据，可点击右上角创建任务" />
      </div>
    </div>

    <!-- 创建任务对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建音频任务" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="创建方式">
          <el-radio-group v-model="form.createMode">
            <el-radio-button label="upload">本地上传</el-radio-button>
            <el-radio-button label="path">填写路径</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="频道ID" prop="channelId">
          <el-input-number v-model="form.channelId" :min="1" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="form.createMode === 'path'" label="原始音频路径" prop="originalFilePath">
          <el-input v-model="form.originalFilePath" placeholder="/data/audio/test1.wav" />
        </el-form-item>
        <el-form-item v-else label="本地音频文件" prop="audioFile">
          <div class="upload-field">
            <label class="upload-btn">
              <input type="file" accept="audio/*,.wav,.mp3,.m4a,.flac" class="upload-input" @change="onFileChange" />
              <span class="upload-btn__text" :title="form.audioFile ? form.audioFile.name : '选择本地音频文件'">
                {{ form.audioFile ? form.audioFile.name : '选择本地音频文件' }}
              </span>
            </label>
            <div class="upload-hint">上传后自动保存到 backend 本地目录并创建任务</div>
          </div>
        </el-form-item>
        <el-form-item label="时长(秒)" prop="duration">
          <el-input-number v-model="form.duration" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="onCreate">创建</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createAudioTask, deleteAudioTask, executeAudioTask, getAudioTasks, uploadAudioTask } from '../api/audioTask'
import PageContainer from '../components/PageContainer.vue'
import EmptyState from '../components/EmptyState.vue'
import StatusTag from '../components/StatusTag.vue'
import RiskTag from '../components/RiskTag.vue'

const router = useRouter()
const loading = ref(false)
const creating = ref(false)
const createDialogVisible = ref(false)
const executingId = ref(null)
const deletingId = ref(null)
const list = ref([])

const formRef = ref()
const form = reactive({
  createMode: 'upload',
  channelId: 1,
  originalFilePath: '/data/audio/test_high.wav',
  audioFile: null,
  duration: 20
})

const rules = {
  channelId: [{ required: true, message: '请输入频道ID', trigger: 'change' }],
  originalFilePath: [{
    validator: (_rule, value, callback) => {
      if (form.createMode === 'path' && !value) {
        callback(new Error('请输入文件路径'))
        return
      }
      callback()
    },
    trigger: 'blur'
  }],
  audioFile: [{
    validator: (_rule, _value, callback) => {
      if (form.createMode === 'upload' && !form.audioFile) {
        callback(new Error('请选择本地音频文件'))
        return
      }
      callback()
    },
    trigger: 'change'
  }]
}

function rowClassName({ row }) {
  const risk = String(row.riskLevel || '').toUpperCase()
  return risk === 'HIGH' ? 'row-risk-high' : ''
}

function getPipelineClass(status) {
  const s = String(status || '').toUpperCase()
  if (s === 'COMPLETED' || s === 'SUCCESS') return 'badge-done'
  if (s === 'RUNNING' || s === 'PROCESSING') return 'badge-running'
  if (s === 'FAILED' || s === 'ERROR') return 'badge-error'
  return 'badge-idle'
}

function formatStatus(status) {
  const s = String(status || '').toUpperCase()
  const map = { COMPLETED: '完成', SUCCESS: '完成', RUNNING: '运行中', PROCESSING: '处理中', FAILED: '失败', ERROR: '错误', PENDING: '等待', NOT_STARTED: '未开始' }
  return map[s] || (status || '-')
}

function canExecute(row) {
  return String(row?.taskType || '').toUpperCase() !== 'REALTIME'
}

async function fetchList() {
  loading.value = true
  try {
    list.value = (await getAudioTasks()) || []
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/audio-tasks/${id}`)
}

async function onCreate() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  creating.value = true
  try {
    if (form.createMode === 'upload') {
      const formData = new FormData()
      formData.append('channelId', String(form.channelId))
      if (form.duration != null) {
        formData.append('duration', String(form.duration))
      }
      formData.append('audio', form.audioFile)
      await uploadAudioTask(formData)
    } else {
      await createAudioTask({
        channelId: form.channelId,
        originalFilePath: form.originalFilePath,
        duration: form.duration
      })
    }
    ElMessage.success('任务创建成功')
    createDialogVisible.value = false
    resetForm()
    await fetchList()
  } finally {
    creating.value = false
  }
}

function onFileChange(event) {
  form.audioFile = event?.target?.files?.[0] || null
}

function resetForm() {
  form.createMode = 'upload'
  form.channelId = 1
  form.originalFilePath = '/data/audio/test_high.wav'
  form.audioFile = null
  form.duration = 20
}

async function onExecute(row) {
  if (!canExecute(row)) {
    ElMessage.warning('实时监听任务不支持离线执行，请在值守监控中心继续处理')
    return
  }
  executingId.value = row.id
  try {
    await executeAudioTask(row.id)
    ElMessage.success('任务执行完成')
    await fetchList()
  } finally {
    executingId.value = null
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确认删除任务 #${row.id} 吗？`, '提示', { type: 'warning' })
  deletingId.value = row.id
  try {
    await deleteAudioTask(row.id)
    ElMessage.success('删除成功')
    await fetchList()
  } finally {
    deletingId.value = null
  }
}

onMounted(fetchList)
</script>

<style scoped>
.tasks-page {
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
  flex-wrap: wrap;
  gap: 10px;
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
  background: #0ea5e9;
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

/* 流水线图例 */
.pipeline-legend {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-label {
  font-size: 12px;
  color: #94a3b8;
}

.pipeline-step {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 4px;
  letter-spacing: 0.5px;
}

.step-se { background: #e0f2fe; color: #0369a1; }
.step-asr { background: #ede9fe; color: #6d28d9; }
.step-llm { background: #fef3c7; color: #b45309; }

.arrow {
  color: #cbd5e1;
  font-size: 12px;
}

.panel-body {
  padding: 10px 14px 14px;
}

.table-scroll {
  overflow-x: auto;
}

/* ===== 流水线徽章 ===== */
.pipeline-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pipeline-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 10px;
  border-radius: 6px;
  min-width: 56px;
  border: 1px solid transparent;
}

.badge-step {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.5px;
  line-height: 1;
  margin-bottom: 2px;
}

.badge-status {
  font-size: 11px;
  line-height: 1;
}

.badge-done    { background: #f0fdf4; border-color: #bbf7d0; color: #15803d; }
.badge-running { background: #eff6ff; border-color: #bfdbfe; color: #1d4ed8; }
.badge-error   { background: #fff1f2; border-color: #fecdd3; color: #b91c1c; }
.badge-idle    { background: #f8fafc; border-color: #e2e8f0; color: #94a3b8; }

.pipe-arrow {
  color: #d1d5db;
  font-size: 12px;
}

/* ===== 表格 ===== */
:deep(.tasks-table.el-table) {
  --el-table-border-color: #f1f5f9;
  --el-table-header-bg-color: #fafbfc;
  --el-table-header-text-color: #64748b;
  --el-table-row-hover-bg-color: #f8faff;
  font-size: 13px;
}

:deep(.tasks-table.el-table th.el-table__cell) {
  font-weight: 600;
  font-size: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #e8edf5 !important;
}

:deep(.tasks-table.el-table td.el-table__cell) {
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9 !important;
  color: #334155;
}

:deep(.tasks-table.el-table::before) {
  display: none;
}

:deep(.row-risk-high td) {
  background: rgba(254, 226, 226, 0.35) !important;
}

:deep(.row-risk-high td:first-child) {
  border-left: 3px solid #ef4444;
}

.action-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.upload-field {
  width: 100%;
}

.upload-btn {
  display: flex;
  align-items: center;
  min-height: 40px;
  width: 100%;
  padding: 0 14px;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  cursor: pointer;
  color: #334155;
  background: #f8fafc;
  overflow: hidden;
}

.upload-btn__text {
  display: block;
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-input {
  display: none;
}

.upload-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
