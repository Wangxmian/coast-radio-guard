import request from './request'

export function getStructuredResults(params = {}) {
  return request.get('/structured-results', { params })
}

export function getStructuredResultDetail(taskId) {
  return request.get(`/structured-results/${taskId}`)
}

export function getStructuredResultJson(taskId) {
  return request.get(`/structured-results/${taskId}/json`)
}
