import request from '@/utils/request'

/**
 * 获取告警列表（分页）
 */
export function getAlertList(params) {
  return request({
    url: '/alerts',
    method: 'get',
    params
  })
}

/**
 * 获取未解决的告警
 */
export function getUnresolvedAlerts() {
  return request({
    url: '/alerts/unresolved',
    method: 'get'
  })
}

/**
 * 获取最近的告警
 */
export function getRecentAlerts(limit = 10) {
  return request({
    url: '/alerts/recent',
    method: 'get',
    params: { limit }
  })
}

/**
 * 按类型获取告警
 */
export function getAlertsByType(alertType) {
  return request({
    url: `/alerts/type/${alertType}`,
    method: 'get'
  })
}

/**
 * 解决告警
 */
export function resolveAlert(id) {
  return request({
    url: `/alerts/${id}/resolve`,
    method: 'post'
  })
}

/**
 * 批量解决设备的告警
 */
export function resolveDeviceAlerts(deviceId) {
  return request({
    url: `/alerts/device/${deviceId}/resolve-all`,
    method: 'post'
  })
}

/**
 * 获取告警统计信息
 */
export function getAlertStatistics() {
  return request({
    url: '/alerts/statistics',
    method: 'get'
  })
}
