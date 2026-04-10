import request from './request'

export function getChannels() {
  return request.get('/channels')
}

export function getChannelDetail(id) {
  return request.get(`/channels/${id}`)
}

export function createChannel(data) {
  return request.post('/channels', data)
}

export function updateChannel(id, data) {
  return request.put(`/channels/${id}`, data)
}

export function deleteChannel(id) {
  return request.delete(`/channels/${id}`)
}
