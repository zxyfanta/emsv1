import request from '@/utils/request'

// 获取设备列表
export const getDeviceList = (params) => {
  return request({
    url: '/devices',
    method: 'get',
    params
  })
}

// 获取设备详情
export const getDeviceDetail = (id) => {
  return request({
    url: `/devices/${id}`,
    method: 'get'
  })
}

// 创建设备
export const createDevice = (data) => {
  return request({
    url: '/devices',
    method: 'post',
    data
  })
}

// 更新设备
export const updateDevice = (id, data) => {
  return request({
    url: `/devices/${id}`,
    method: 'put',
    data
  })
}

// 删除设备
export const deleteDevice = (id) => {
  return request({
    url: `/devices/${id}`,
    method: 'delete'
  })
}

// 更新设备状态
export const updateDeviceStatus = (id, status) => {
  return request({
    url: `/devices/${id}/status`,
    method: 'patch',
    params: { status }
  })
}

// 获取设备统计数据
export const getDeviceStatistics = () => {
  return request({
    url: '/devices/statistics',
    method: 'get'
  })
}

// 获取所有设备（用于可视化大屏）
export const getAllDevices = (params = {}) => {
  return request({
    url: '/devices',
    method: 'get',
    params: { size: 1000, ...params }  // 合并参数
  })
}

// 获取辐射设备最新数据
export const getRadiationDeviceLatestData = (deviceCode) => {
  return request({
    url: '/radiation-data/latest',
    method: 'get',
    params: { deviceCode }
  })
}

// 获取环境设备最新数据
export const getEnvironmentDeviceLatestData = (deviceCode) => {
  return request({
    url: '/environment-data/latest',
    method: 'get',
    params: { deviceCode }
  })
}

// ============ 设备激活相关接口 ============

// 验证激活码
export const verifyActivationCode = (code) => {
  return request({
    url: '/devices/verify-activation-code',
    method: 'post',
    data: { activationCode: code }
  })
}

// 使用激活码激活设备
export const activateDevice = (data) => {
  return request({
    url: '/devices/activate',
    method: 'post',
    data
  })
}

// ============ 管理员接口 ============

// 批量导入设备
export const batchImportDevices = (items) => {
  return request({
    url: '/admin/devices/batch-import',
    method: 'post',
    data: { items }
  })
}

// 获取设备的激活码
export const getDeviceActivationCode = (deviceId) => {
  return request({
    url: `/devices/${deviceId}/activation-code`,
    method: 'get'
  })
}

// ============ 数据上报相关接口 ============

// 获取设备上报日志
export const getDeviceReportLogs = (deviceId, params) => {
  return request({
    url: `/devices/${deviceId}/report-logs`,
    method: 'get',
    params
  })
}

// 获取系统上报配置（管理员专用）
export const getSystemReportConfig = () => {
  return request({
    url: '/admin/system/report-config',
    method: 'get'
  })
}

// 更新系统上报配置（管理员专用）
export const updateSystemReportConfig = (data) => {
  return request({
    url: '/admin/system/report-config',
    method: 'put',
    data
  })
}
