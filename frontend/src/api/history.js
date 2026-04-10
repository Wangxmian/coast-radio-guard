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

export function getHistoryRecords(params = {}) {
  return request.get('/history-records', { params })
}

export async function exportHistoryRecords(params = {}) {
  const resp = await fileClient.get('/history-records/export', {
    params,
    responseType: 'blob'
  })
  return resp.data
}
