import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/monitor-center',
    children: [
      {
        path: '/dashboard',
        name: 'dashboard',
        redirect: '/monitor-center'
      },
      {
        path: '/monitor-center',
        name: 'monitorCenter',
        component: () => import('../views/MonitorCenterView.vue'),
        meta: { title: '值守监控中心' }
      },
      {
        path: '/channels',
        name: 'channels',
        component: () => import('../views/ChannelsView.vue'),
        meta: { title: '频道管理' }
      },
      {
        path: '/audio-tasks',
        name: 'audioTasks',
        component: () => import('../views/AudioTasksView.vue'),
        meta: { title: '音频任务' }
      },
      {
        path: '/history-records',
        name: 'historyRecords',
        component: () => import('../views/HistoryRecordsView.vue'),
        meta: { title: '历史记录' }
      },
      {
        path: '/analysis-reports',
        name: 'analysisReports',
        component: () => import('../views/StructuredResultsView.vue'),
        meta: { title: '分析报告' }
      },
      {
        path: '/structured-results',
        redirect: '/analysis-reports'
      },
      {
        path: '/audio-tasks/:id',
        name: 'audioTaskDetail',
        component: () => import('../views/AudioTaskDetailView.vue'),
        meta: { title: '任务分析详情' }
      },
      {
        path: '/alarms',
        name: 'alarms',
        component: () => import('../views/AlarmsView.vue'),
        meta: { title: '告警中心' }
      },
      {
        path: '/agent-chat',
        name: 'agentChat',
        component: () => import('../views/AgentChatView.vue'),
        meta: { title: '智能体对话' }
      },
      {
        path: '/system-settings',
        name: 'systemSettings',
        component: () => import('../views/SystemSettingsView.vue'),
        meta: { title: '系统设置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('crg_token')
  const isPublic = Boolean(to.meta.public)

  if (!isPublic && !token) {
    next('/login')
    return
  }

  if (to.path === '/login' && token) {
    next('/monitor-center')
    return
  }

  next()
})

export default router
