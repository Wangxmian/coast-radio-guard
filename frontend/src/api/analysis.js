import request from './request'

export function getAudioTaskAnalysis(id) {
  return request.get(`/audio-tasks/${id}/analysis`)
}
