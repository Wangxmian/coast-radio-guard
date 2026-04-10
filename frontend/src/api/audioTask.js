import request from './request'

export function getAudioTasks() {
  return request.get('/audio-tasks')
}

export function getAudioTaskDetail(id) {
  return request.get(`/audio-tasks/${id}`)
}

export function createAudioTask(data) {
  return request.post('/audio-tasks', data)
}

export function uploadAudioTask(formData) {
  return request.post('/audio-tasks/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000
  })
}

export function updateAudioTask(id, data) {
  return request.put(`/audio-tasks/${id}`, data)
}

export function deleteAudioTask(id) {
  return request.delete(`/audio-tasks/${id}`)
}

export function executeAudioTask(id) {
  return request.post(`/audio-tasks/${id}/execute`)
}
