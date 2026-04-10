import request from './request'

export function getSystemConfigs() {
  return request.get('/system-configs')
}

export function getGroupedSystemConfigs() {
  return request.get('/system-configs/grouped')
}

export function updateSystemConfigs(configs) {
  return request.put('/system-configs', { configs })
}
