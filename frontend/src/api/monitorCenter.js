import request from './request'

export function getMonitorCenterOverview() {
  return request.get('/monitor-center/overview')
}
