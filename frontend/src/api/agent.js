import request from './request'

export function agentChat(message) {
  return request.post('/agent/chat', { message })
}

export function agentReport(type) {
  return request.post('/agent/report', { type })
}

