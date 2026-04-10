import axios from 'axios'
import request from './request'

const envBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').trim()
const normalizedEnvBaseUrl = envBaseUrl ? envBaseUrl.replace(/\/$/, '') : ''

const fileClient = axios.create({
  baseURL: normalizedEnvBaseUrl ? `${normalizedEnvBaseUrl}/api` : '/backend-api',
  timeout: 30000
})

fileClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('crg_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function getAlarmList(params = {}) {
  return request.get('/alarms', { params })
}

export function getAlarmDetail(id) {
  return request.get(`/alarms/${id}`)
}

export function createManualAlarm(data) {
  return request.post('/alarms/manual', data)
}

export function ackAlarm(id, remark) {
  return request.post(`/alarms/${id}/ack`, { remark })
}

export function processAlarm(id, remark) {
  return request.post(`/alarms/${id}/process`, { remark })
}

export function resolveAlarm(id, remark) {
  return request.post(`/alarms/${id}/resolve`, { remark })
}

export function closeAlarm(id, remark) {
  return request.post(`/alarms/${id}/close`, { remark })
}

export function falseAlarm(id, remark) {
  return request.post(`/alarms/${id}/false-alarm`, { remark })
}

export async function exportAlarms(params = {}) {
  const resp = await fileClient.get('/alarms/export', {
    params,
    responseType: 'blob'
  })
  return resp.data
}
