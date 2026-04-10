<template>
  <PageContainer class="crg-page settings-page" v-loading="loading">
    <div class="page-header-bar">
      <div>
        <div class="page-eyebrow">SYSTEM SETTINGS</div>
        <h1 class="page-title">系统设置</h1>
        <p class="page-desc">管理值守参数与模型策略配置，通知方式/数据保留策略为预留项。</p>
      </div>
      <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
    </div>

    <div class="grid">
      <SectionPanel title="识别与告警参数" subtitle="已接入 /api/system-configs" body-padding="16px">
        <el-form label-position="top">
          <el-form-item label="热词词典">
            <el-input v-model="form.hotwordDictionary" type="textarea" :rows="6" placeholder="每行一个关键词" />
          </el-form-item>
          <el-form-item label="风险阈值">
            <el-slider v-model="form.riskThreshold" :min="0" :max="100" show-input />
          </el-form-item>
          <el-form-item label="自动报警">
            <el-switch v-model="form.autoAlarmEnabled" />
          </el-form-item>
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="转录纠错">
                <el-switch v-model="form.correctionEnabled" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="分析优先用纠错文本">
                <el-switch v-model="form.analysisUseCorrectedTranscript" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="模型配置描述">
            <el-input v-model="form.modelDescription" placeholder="例如：VAD+SE+ASR+LLM pipeline" />
          </el-form-item>
          <el-row :gutter="12">
            <el-col :span="8">
              <el-form-item label="VAD 启用">
                <el-switch v-model="form.vadEnabled" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="ASR 启用">
                <el-switch v-model="form.asrEnabled" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="LLM 启用">
                <el-switch v-model="form.llmEnabled" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </SectionPanel>

      <SectionPanel title="通知方式（预留）" subtitle="待后端通知中心接入" body-padding="16px">
        <el-alert type="info" :closable="false" show-icon title="通知通道尚未接入" description="后续可扩展短信、企业微信、邮件、Webhook 等告警通知方式。" />
      </SectionPanel>

      <SectionPanel title="数据保留策略（预留）" subtitle="待归档策略接入" body-padding="16px">
        <el-alert type="warning" :closable="false" show-icon title="当前使用默认保留策略" description="后续可配置音频留存天数、转写留存天数与归档清理任务。" />
      </SectionPanel>
    </div>
  </PageContainer>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageContainer from '../components/PageContainer.vue'
import SectionPanel from '../components/SectionPanel.vue'
import { getSystemConfigs, updateSystemConfigs } from '../api/systemConfig'

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  hotwordDictionary: '',
  riskThreshold: 70,
  autoAlarmEnabled: true,
  correctionEnabled: true,
  analysisUseCorrectedTranscript: true,
  modelDescription: '',
  vadEnabled: true,
  asrEnabled: true,
  llmEnabled: true
})

async function load() {
  loading.value = true
  try {
    const list = await getSystemConfigs()
    const map = Object.fromEntries((Array.isArray(list) ? list : []).map((it) => [it.configKey, it.configValue]))
    form.hotwordDictionary = map.hotwordDictionary || ''
    form.riskThreshold = Number(map.riskThreshold || 70)
    form.autoAlarmEnabled = String(map.autoAlarmEnabled || 'true').toLowerCase() === 'true'
    form.correctionEnabled = String(map.correctionEnabled || 'true').toLowerCase() === 'true'
    form.analysisUseCorrectedTranscript = String(map.analysisUseCorrectedTranscript || 'true').toLowerCase() === 'true'
    form.modelDescription = map.modelDescription || ''
    form.vadEnabled = String(map.vadEnabled || 'true').toLowerCase() === 'true'
    form.asrEnabled = String(map.asrEnabled || 'true').toLowerCase() === 'true'
    form.llmEnabled = String(map.llmEnabled || 'true').toLowerCase() === 'true'
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await updateSystemConfigs([
      { configKey: 'hotwordDictionary', configValue: form.hotwordDictionary || '' },
      { configKey: 'riskThreshold', configValue: String(form.riskThreshold ?? 70) },
      { configKey: 'autoAlarmEnabled', configValue: String(Boolean(form.autoAlarmEnabled)) },
      { configKey: 'correctionEnabled', configValue: String(Boolean(form.correctionEnabled)) },
      { configKey: 'analysisUseCorrectedTranscript', configValue: String(Boolean(form.analysisUseCorrectedTranscript)) },
      { configKey: 'modelDescription', configValue: form.modelDescription || '' },
      { configKey: 'vadEnabled', configValue: String(Boolean(form.vadEnabled)) },
      { configKey: 'asrEnabled', configValue: String(Boolean(form.asrEnabled)) },
      { configKey: 'llmEnabled', configValue: String(Boolean(form.llmEnabled)) }
    ])
    ElMessage.success('系统设置已保存')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.settings-page {
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

.grid {
  display: grid;
  gap: 14px;
}
</style>
