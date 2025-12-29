import request from '@/utils/request'

// 获取辐射数据列表
export const getRadiationDataList = (params) => {
  return request({
    url: '/radiation-data',
    method: 'get',
    params
  })
}

// 获取最新辐射数据
export const getLatestRadiationData = (deviceCode) => {
  return request({
    url: '/radiation-data/latest',
    method: 'get',
    params: { deviceCode }
  })
}

// 获取辐射数据统计
export const getRadiationStatistics = (params) => {
  return request({
    url: '/radiation-data/statistics',
    method: 'get',
    params
  })
}

// 获取环境数据列表
export const getEnvironmentDataList = (params) => {
  return request({
    url: '/environment-data',
    method: 'get',
    params
  })
}

// 获取最新环境数据
export const getLatestEnvironmentData = (deviceCode) => {
  return request({
    url: '/environment-data/latest',
    method: 'get',
    params: { deviceCode }
  })
}

// 获取环境数据统计
export const getEnvironmentStatistics = (params) => {
  return request({
    url: '/environment-data/statistics',
    method: 'get',
    params
  })
}

// 获取设备上报日志
export const getDeviceReportLogs = (deviceId, params) => {
  return request({
    url: `/devices/${deviceId}/report-logs`,
    method: 'get',
    params
  })
}

