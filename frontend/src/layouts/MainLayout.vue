<template>
  <el-container class="layout-root">
    <el-aside width="250px" class="sidebar">
      <div class="brand">
        <div class="brand-mark">CRG</div>
        <div class="brand-text">
          <h1>海岸电台值守</h1>
          <p>Coast Radio Guard</p>
        </div>
      </div>

      <el-menu :default-active="activeMenu" router class="menu" background-color="transparent" text-color="#c7d5e3" active-text-color="#ffffff">
        <div class="menu-group-title">监控</div>
        <el-menu-item index="/monitor-center">
          <span class="menu-title">值守监控中心</span>
          <span class="menu-sub">态势感知与实时监听中枢</span>
        </el-menu-item>

        <div class="menu-group-title">任务</div>
        <el-menu-item index="/audio-tasks">
          <span class="menu-title">音频任务</span>
          <span class="menu-sub">任务执行与分析链路</span>
        </el-menu-item>
        <el-menu-item index="/history-records">
          <span class="menu-title">历史记录</span>
          <span class="menu-sub">按时间与文本检索记录</span>
        </el-menu-item>
        <el-menu-item index="/analysis-reports">
          <span class="menu-title">分析报告</span>
          <span class="menu-sub">批量检索分析结果报告</span>
        </el-menu-item>

        <div class="menu-group-title">告警</div>
        <el-menu-item index="/alarms">
          <span class="menu-title">告警中心</span>
          <span class="menu-sub">风险告警与处置观察</span>
        </el-menu-item>

        <div class="menu-group-title">配置</div>
        <el-menu-item index="/channels">
          <span class="menu-title">频道管理</span>
          <span class="menu-sub">电台频道配置维护</span>
        </el-menu-item>
        <el-menu-item index="/system-settings">
          <span class="menu-title">系统设置</span>
          <span class="menu-sub">识别阈值与策略配置</span>
        </el-menu-item>

        <div class="menu-group-title">工具</div>
        <el-menu-item index="/agent-chat">
          <span class="menu-title">智能体对话 <span class="beta-tag">Beta</span></span>
          <span class="menu-sub">自然语言分析与报告</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="topbar">
        <div>
          <div class="topbar-title">{{ currentTitle }}</div>
          <div class="topbar-sub">海岸电台智能离线值守系统</div>
        </div>
        <div class="user-actions">
          <el-button
            v-if="latestHighAlarm"
            type="danger"
            plain
            size="small"
            class="alarm-chip"
            @click="goAlarm(latestHighAlarm.id)"
          >
            高风险告警 #{{ latestHighAlarm.id }}
          </el-button>
          <div class="user-badge">
            <span class="dot"></span>
            {{ userStore.userInfo?.username || 'unknown' }}
          </div>
          <el-button type="danger" plain size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage, ElNotification } from 'element-plus'
import { useUserStore } from '../stores/user'
import { getAlarmList } from '../api/alarm'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const latestHighAlarm = ref(null)
const lastAlertSoundAt = ref(0)
let alarmPollTimer = null

const activeMenu = computed(() => {
  if (route.path.startsWith('/audio-tasks/')) {
    return '/audio-tasks'
  }
  return route.path
})

const currentTitle = computed(() => route.meta?.title || '系统页面')

function goAlarm(alarmId) {
  router.push({ path: '/alarms', query: alarmId ? { alarmId: String(alarmId) } : {} })
}

function playAlertTone() {
  const now = Date.now()
  if (now - lastAlertSoundAt.value < 12000) {
    return
  }
  lastAlertSoundAt.value = now
  const AudioContextCtor = window.AudioContext || window.webkitAudioContext
  if (!AudioContextCtor) {
    return
  }
  const ctx = new AudioContextCtor()
  const gain = ctx.createGain()
  gain.connect(ctx.destination)
  gain.gain.setValueAtTime(0.0001, ctx.currentTime)
  gain.gain.exponentialRampToValueAtTime(0.18, ctx.currentTime + 0.02)
  gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + 0.55)
  const osc = ctx.createOscillator()
  osc.type = 'square'
  osc.frequency.setValueAtTime(880, ctx.currentTime)
  osc.connect(gain)
  osc.start()
  osc.stop(ctx.currentTime + 0.55)
  osc.onended = () => ctx.close().catch(() => {})
}

async function pollLatestAlarm() {
  const alarms = await getAlarmList({
    alarmLevel: 'HIGH',
    alarmStatus: 'UNHANDLED',
    page: 1,
    pageSize: 1
  })
  const latest = Array.isArray(alarms) && alarms.length ? alarms[0] : null
  latestHighAlarm.value = latest
  if (!latest?.id) {
    return
  }
  const storageKey = 'crg_last_notified_alarm_id'
  const lastNotifiedId = Number(sessionStorage.getItem(storageKey) || '0')
  if (latest.id > lastNotifiedId) {
    sessionStorage.setItem(storageKey, String(latest.id))
    ElNotification({
      title: '高风险自动告警',
      type: 'error',
      duration: 8000,
      message: latest.triggerReason || `任务 #${latest.taskId || '-'} 触发高风险告警`,
      onClick: () => goAlarm(latest.id)
    })
    playAlertTone()
  }
}

async function handleLogout() {
  await ElMessageBox.confirm('确认退出登录吗？', '提示', { type: 'warning' })
  userStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

onMounted(() => {
  pollLatestAlarm().catch(() => {})
  alarmPollTimer = window.setInterval(() => {
    pollLatestAlarm().catch(() => {})
  }, 10000)
})

onBeforeUnmount(() => {
  if (alarmPollTimer) {
    window.clearInterval(alarmPollTimer)
  }
})
</script>

<style scoped>
.layout-root {
  min-height: 100vh;
  background: var(--crg-bg-page);
}

.sidebar {
  position: sticky;
  top: 0;
  align-self: flex-start;
  height: 100vh;
  overflow-y: auto;
  background: linear-gradient(180deg, #0e2031 0%, #14293e 100%);
  color: #fff;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1d6aa0, #144066);
  color: #fff;
  font-weight: 700;
  font-size: 12px;
  display: grid;
  place-items: center;
  letter-spacing: 0.4px;
}

.brand-text h1 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #f2f7fc;
}

.brand-text p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #a7bfd4;
}

.menu {
  border-right: none;
  padding: 10px;
}

.menu-group-title {
  margin: 10px 8px 8px;
  color: #8ea9bf;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

:deep(.el-menu-item) {
  height: auto;
  min-height: 54px;
  border-radius: 10px;
  margin-bottom: 8px;
  padding: 10px 14px !important;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  line-height: 1.2;
}

:deep(.el-menu-item.is-active) {
  background: rgba(47, 110, 156, 0.36) !important;
  box-shadow: inset 0 0 0 1px rgba(132, 182, 219, 0.38);
}

.menu-title {
  font-size: 14px;
  font-weight: 600;
  position: relative;
  padding-left: 12px;
}

.menu-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 6px;
  height: 6px;
  transform: translateY(-50%);
  border-radius: 2px;
  background: rgba(182, 208, 230, 0.8);
}

.menu-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #95aec4;
}

.beta-tag {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 6px;
  border-radius: 999px;
  border: 1px solid rgba(234, 179, 8, 0.45);
  color: #f8d26f;
  font-size: 10px;
  font-weight: 700;
  vertical-align: middle;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  background: #fbfcfe;
  border-bottom: 1px solid var(--crg-border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 68px;
  padding: 0 24px;
}

.topbar-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--crg-text-main);
}

.topbar-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--crg-text-muted);
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.alarm-chip {
  border-width: 1px;
}

.user-badge {
  height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid var(--crg-border-color);
  background: #fff;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #35516b;
  font-size: 13px;
}

.user-badge .dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #1e8e5a;
}

.main-content {
  min-width: 0;
  padding: 24px;
  background: transparent;
}

@media (max-width: 980px) {
  .sidebar {
    width: 220px !important;
  }

  .main-content {
    padding: 16px;
  }
}

@media (max-width: 760px) {
  .topbar {
    padding: 0 12px;
  }

  .topbar-sub {
    display: none;
  }
}
</style>
