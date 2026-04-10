import request from './request'

export function startRealtime(data = {}) {
  return request.post('/realtime/start', data)
}

export function stopRealtime() {
  return request.post('/realtime/stop')
}

export function getRealtimeStatus() {
  return request.get('/realtime/status')
}

export function sendRealtimeChunk(formData) {
  return request.post('/realtime/chunk', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000
  })
}
