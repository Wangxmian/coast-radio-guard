import axios from 'axios'
import { ElMessage } from 'element-plus'

const envBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').trim()
const normalizedEnvBaseUrl = envBaseUrl ? envBaseUrl.replace(/\/$/, '') : ''

const request = axios.create({
  // 默认走 Vite 代理，避免 127.0.0.1 与 localhost 的跨域/网络问题
  baseURL: normalizedEnvBaseUrl ? `${normalizedEnvBaseUrl}/api` : '/backend-api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('crg_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && res.success === true) {
      return res.data
    }
    const message = res?.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  (error) => {
    const status = error?.response?.status
    const message = error?.response?.data?.message || error?.message || '网络异常'
    if (status === 401) {
      localStorage.removeItem('crg_token')
      localStorage.removeItem('crg_user_info')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
