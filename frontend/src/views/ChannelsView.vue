<template>
  <PageContainer class="crg-page channels-page" v-loading="loading">
    <div class="page-header-bar">
      <div class="header-left">
        <div class="page-eyebrow">CHANNEL MANAGEMENT</div>
        <h1 class="page-title">频道管理</h1>
        <p class="page-desc">维护海岸电台频道基础信息与值守优先级</p>
      </div>
      <div class="header-actions">
        <el-button @click="fetchList">刷新</el-button>
        <el-button type="primary" @click="openCreate">+ 新增频道</el-button>
      </div>
    </div>

    <div class="data-panel">
      <div class="panel-head">
        <div class="panel-head-left">
          <span class="panel-accent"></span>
          <span class="panel-title">频道列表</span>
          <span class="panel-sub">支持新增、编辑与删除</span>
        </div>
        <div class="panel-count" v-if="list.length">共 {{ list.length }} 个频道</div>
      </div>
      <div class="panel-body">
        <template v-if="list.length">
          <el-table :data="list" empty-text="暂无频道数据" class="channels-table">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="channelCode" label="频道编码" min-width="120" />
            <el-table-column prop="channelName" label="频道名称" min-width="130" />
            <el-table-column prop="frequency" label="频段" min-width="110" />
            <el-table-column label="优先级" width="100">
              <template #default="scope">
                <div class="priority-badge" :class="`priority-${Math.min(scope.row.priority || 5, 10) > 7 ? 'high' : Math.min(scope.row.priority || 5, 10) > 4 ? 'mid' : 'low'}`">
                  P{{ scope.row.priority || '-' }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="scope">
                <StatusTag :status="scope.row.status === 1 ? 'ENABLED' : 'DISABLED'" />
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" min-width="165" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="scope">
                <div class="op-row">
                  <el-button size="small" text type="primary" @click="openEdit(scope.row)">编辑</el-button>
                  <el-button size="small" text type="danger" :loading="deletingId === scope.row.id" @click="onDelete(scope.row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>
        <EmptyState v-else description="暂无频道数据，可点击右上角新增频道" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑频道' : '新增频道'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-if="!isEdit" label="频道编码" prop="channelCode">
          <el-input v-model="form.channelCode" placeholder="如 CH-001" />
        </el-form-item>
        <el-form-item label="频道名称" prop="channelName">
          <el-input v-model="form.channelName" placeholder="频道中文名称" />
        </el-form-item>
        <el-form-item label="频段" prop="frequency">
          <el-input v-model="form.frequency" placeholder="如 156.8 MHz" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createChannel, deleteChannel, getChannels, updateChannel } from '../api/channel'
import PageContainer from '../components/PageContainer.vue'
import PageHeader from '../components/PageHeader.vue'
import SectionPanel from '../components/SectionPanel.vue'
import EmptyState from '../components/EmptyState.vue'
import StatusTag from '../components/StatusTag.vue'

const loading = ref(false)
const saving = ref(false)
const deletingId = ref(null)
const list = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref(null)
const formRef = ref()
const form = reactive({
  channelCode: '',
  channelName: '',
  frequency: '',
  priority: 1,
  status: 1,
  remark: ''
})

const rules = {
  channelCode: [{ required: true, message: '请输入频道编码', trigger: 'blur' }],
  channelName: [{ required: true, message: '请输入频道名称', trigger: 'blur' }],
  frequency: [{ required: true, message: '请输入频段', trigger: 'blur' }],
  priority: [{ required: true, message: '请输入优先级', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function resetForm() {
  form.channelCode = ''
  form.channelName = ''
  form.frequency = ''
  form.priority = 1
  form.status = 1
  form.remark = ''
}

function openCreate() {
  isEdit.value = false
  currentId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  currentId.value = row.id
  form.channelCode = row.channelCode
  form.channelName = row.channelName
  form.frequency = row.frequency
  form.priority = row.priority
  form.status = row.status
  form.remark = row.remark || ''
  dialogVisible.value = true
}

async function fetchList() {
  loading.value = true
  try {
    list.value = (await getChannels()) || []
  } finally {
    loading.value = false
  }
}

async function onSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    if (isEdit.value) {
      await updateChannel(currentId.value, {
        channelName: form.channelName,
        frequency: form.frequency,
        priority: form.priority,
        status: form.status,
        remark: form.remark
      })
      ElMessage.success('更新成功')
    } else {
      await createChannel({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await fetchList()
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm('确认删除频道 ' + row.channelName + ' 吗？', '提示', { type: 'warning' })
  deletingId.value = row.id
  try {
    await deleteChannel(row.id)
    ElMessage.success('删除成功')
    await fetchList()
  } finally {
    deletingId.value = null
  }
}

onMounted(fetchList)
</script>

<style scoped>
.channels-page {
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
  color: #22c55e;
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
  background: #22c55e;
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

.panel-count {
  font-size: 13px;
  color: #94a3b8;
  background: #f1f5f9;
  padding: 3px 12px;
  border-radius: 20px;
}

.panel-body {
  padding: 10px 14px 14px;
}

/* 优先级标签 */
.priority-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 24px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
}

.priority-high { background: #fee2e2; color: #dc2626; }
.priority-mid  { background: #fef3c7; color: #d97706; }
.priority-low  { background: #f0fdf4; color: #16a34a; }

/* 操作 */
.op-row {
  display: flex;
  gap: 2px;
  align-items: center;
}

/* 表格 */
:deep(.channels-table.el-table) {
  --el-table-border-color: #f1f5f9;
  --el-table-header-bg-color: #fafbfc;
  --el-table-header-text-color: #64748b;
  --el-table-row-hover-bg-color: #f8faff;
  font-size: 13px;
}

:deep(.channels-table.el-table th.el-table__cell) {
  font-weight: 600;
  font-size: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #e8edf5 !important;
}

:deep(.channels-table.el-table td.el-table__cell) {
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9 !important;
  color: #334155;
}

:deep(.channels-table.el-table::before) {
  display: none;
}
</style>