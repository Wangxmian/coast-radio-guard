<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="24" cy="24" r="10" stroke="#38bdf8" stroke-width="2"/>
            <circle cx="24" cy="24" r="18" stroke="#38bdf8" stroke-width="1.5" stroke-dasharray="4 3" opacity="0.6"/>
            <circle cx="24" cy="24" r="24" stroke="#38bdf8" stroke-width="1" stroke-dasharray="3 4" opacity="0.3"/>
            <circle cx="24" cy="24" r="4" fill="#38bdf8"/>
            <line x1="24" y1="0" x2="24" y2="14" stroke="#38bdf8" stroke-width="1.5" opacity="0.8"/>
          </svg>
        </div>
        <h1 class="brand-title">海岸电台<br/>智能离线值守</h1>
        <p class="brand-subtitle">音频任务编排 · 风险分析 · 告警联动</p>
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-dot"></span>
            实时语音识别与 AI 语义分析
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            多频道并发任务调度管理
          </div>
          <div class="feature-item">
            <span class="feature-dot"></span>
            风险事件告警自动触发联动
          </div>
        </div>
      </div>
      <!-- 雷达动效 -->
      <div class="radar-wrap">
        <div class="radar-ring r1"></div>
        <div class="radar-ring r2"></div>
        <div class="radar-ring r3"></div>
        <div class="radar-sweep"></div>
        <div class="radar-dot d1"></div>
        <div class="radar-dot d2"></div>
        <div class="radar-dot d3"></div>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-panel">
      <div class="login-card">
        <div class="card-header">
          <div class="card-badge">SYSTEM LOGIN</div>
          <h2 class="card-title">系统登录</h2>
          <p class="card-desc">请使用您的系统账号登录</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="onSubmit" class="login-form">
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              clearable
              size="large"
              prefix-icon="User"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="请输入密码"
              size="large"
              prefix-icon="Lock"
            />
          </el-form-item>

          <el-button
            type="primary"
            :loading="loading"
            class="submit-btn"
            size="large"
            @click="onSubmit"
          >
            <span>登 录 系 统</span>
          </el-button>
        </el-form>

        <div class="hint">
          <span class="hint-icon">●</span> 默认账号：admin / Admin@123
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'Admin@123'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.replace('/monitor-center')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 480px;
  background: #060f1a;
  overflow: hidden;
}

/* ======== 左侧品牌区 ======== */
.login-brand {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 80px;
  background:
    radial-gradient(ellipse 80% 60% at 30% 50%, rgba(14, 165, 233, 0.08) 0%, transparent 70%),
    linear-gradient(160deg, #0c1e32 0%, #060f1a 100%);
  border-right: 1px solid rgba(56, 189, 248, 0.1);
  overflow: hidden;
}

.brand-content {
  position: relative;
  z-index: 2;
  max-width: 480px;
}

.brand-logo {
  width: 56px;
  height: 56px;
  margin-bottom: 28px;
}

.brand-title {
  margin: 0 0 16px;
  color: #f0f9ff;
  font-size: clamp(32px, 3.2vw, 48px);
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.5px;
}

.brand-subtitle {
  margin: 0 0 40px;
  color: #7dd3fc;
  font-size: 15px;
  letter-spacing: 1px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #94a3b8;
  font-size: 14px;
}

.feature-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #38bdf8;
  flex-shrink: 0;
  box-shadow: 0 0 8px #38bdf8;
}

/* 雷达动效 */
.radar-wrap {
  position: absolute;
  right: -160px;
  bottom: -160px;
  width: 600px;
  height: 600px;
  opacity: 0.18;
}

.radar-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid #38bdf8;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
}

.r1 { width: 200px; height: 200px; }
.r2 { width: 380px; height: 380px; animation: pulse-ring 3s ease-in-out infinite; }
.r3 { width: 560px; height: 560px; animation: pulse-ring 3s ease-in-out 1s infinite; }

.radar-sweep {
  position: absolute;
  top: 50%; left: 50%;
  width: 280px;
  height: 280px;
  transform-origin: 0 0;
  background: conic-gradient(from 0deg, transparent 0%, rgba(56, 189, 248, 0.3) 20%, transparent 21%);
  border-radius: 0 0 0 50%;
  animation: sweep 4s linear infinite;
}

.radar-dot {
  position: absolute;
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #38bdf8;
  box-shadow: 0 0 8px #38bdf8;
}
.d1 { top: 35%; left: 38%; animation: blink 2.8s 0.4s infinite; }
.d2 { top: 48%; left: 62%; animation: blink 2.8s 1.2s infinite; }
.d3 { top: 62%; left: 45%; animation: blink 2.8s 2s infinite; }

@keyframes sweep {
  to { transform: rotate(360deg); }
}
@keyframes pulse-ring {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 0.15; }
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.1; }
}

/* ======== 右侧登录面板 ======== */
.login-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 48px;
  background: #080f1c;
}

.login-card {
  width: 100%;
  max-width: 360px;
}

.card-header {
  margin-bottom: 40px;
}

.card-badge {
  display: inline-block;
  padding: 3px 10px;
  border: 1px solid rgba(56, 189, 248, 0.4);
  border-radius: 4px;
  color: #38bdf8;
  font-size: 11px;
  letter-spacing: 2px;
  font-weight: 600;
  margin-bottom: 20px;
}

.card-title {
  margin: 0 0 8px;
  color: #f1f5f9;
  font-size: 26px;
  font-weight: 700;
}

.card-desc {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

/* 表单深度覆盖 */
.login-form :deep(.el-form-item__label) {
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
  padding-bottom: 6px;
}

.login-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: 8px;
  box-shadow: none !important;
  transition: border-color 0.2s;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(56, 189, 248, 0.4);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #38bdf8;
  background: rgba(56, 189, 248, 0.04);
}

.login-form :deep(.el-input__inner) {
  color: #e2e8f0;
  font-size: 14px;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #475569;
}

.login-form :deep(.el-input__prefix-icon) {
  color: #475569;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 48px;
  border-radius: 8px;
  background: linear-gradient(135deg, #0ea5e9 0%, #0369a1 100%);
  border: none;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 3px;
  transition: all 0.25s;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #38bdf8 0%, #0ea5e9 100%);
  box-shadow: 0 8px 24px rgba(14, 165, 233, 0.35);
  transform: translateY(-1px);
}

.hint {
  margin-top: 28px;
  padding: 12px 16px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  color: #475569;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.hint-icon {
  color: #22c55e;
  font-size: 8px;
}

@media (max-width: 1000px) {
  .login-page {
    grid-template-columns: 1fr;
  }
  .login-brand {
    display: none;
  }
  .login-panel {
    padding: 40px 24px;
    min-height: 100vh;
  }
}
</style>
