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
