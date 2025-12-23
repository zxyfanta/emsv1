import request from '@/utils/request'

/**
 * 视频设备管理API
 *
 * 支持的功能：
 * - 视频设备CRUD管理
 * - 视频设备与监测设备绑定/解绑
 * - 获取视频流URL（支持多种流类型：RTSP/RTMP/HLS/FLV/WebRTC）
 * - 视频流认证信息管理
 */

/**
 * 获取所有视频设备列表（不分页）
 * @returns {Promise}
 */
export function getAllVideoDevices() {
  return request({
    url: '/video-devices/all',
    method: 'get'
  })
}

/**
 * 获取视频设备列表（分页）
 * @param {number} page - 页码
 * @param {number} size - 每页大小
 * @returns {Promise}
 */
export function getVideoDevices(page = 0, size = 20) {
  return request({
    url: '/video-devices',
    method: 'get',
    params: { page, size }
  })
}

/**
 * 获取视频设备详情
 * @param {number} id - 视频设备ID
 * @returns {Promise}
 */
export function getVideoDeviceById(id) {
  return request({
    url: `/video-devices/${id}`,
    method: 'get'
  })
}

/**
 * 创建视频设备
 * @param {Object} data - 视频设备数据
 * @param {string} data.deviceCode - 设备编码（唯一）
 * @param {string} data.deviceName - 设备名称
 * @param {string} data.streamUrl - 视频流URL
 * @param {string} data.streamType - 流类型：RTSP, RTMP, HLS, FLV, WEBRTC
 * @param {string} data.snapshotUrl - 截图URL（可选）
 * @param {string} data.username - 认证用户名（可选）
 * @param {string} data.password - 认证密码（可选）
 * @param {string} data.resolution - 分辨率（可选）
 * @param {number} data.fps - 帧率（可选）
 * @returns {Promise}
 */
export function createVideoDevice(data) {
  return request({
    url: '/video-devices',
    method: 'post',
    data
  })
}

/**
 * 更新视频设备
 * @param {number} id - 视频设备ID
 * @param {Object} data - 更新数据
 * @returns {Promise}
 */
export function updateVideoDevice(id, data) {
  return request({
    url: `/video-devices/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除视频设备
 * @param {number} id - 视频设备ID
 * @returns {Promise}
 */
export function deleteVideoDevice(id) {
  return request({
    url: `/video-devices/${id}`,
    method: 'delete'
  })
}

/**
 * 获取视频流URL（带认证信息）
 * 用于前端播放器直接播放
 * @param {number} id - 视频设备ID
 * @returns {Promise} 返回包含 streamUrl 的对象
 */
export function getVideoStreamUrl(id) {
  return request({
    url: `/video-devices/${id}/stream-url`,
    method: 'get'
  })
}

/**
 * 绑定视频设备到监测设备
 * @param {number} videoDeviceId - 视频设备ID
 * @param {number} monitorDeviceId - 监测设备ID
 * @returns {Promise}
 */
export function bindVideoToDevice(videoDeviceId, monitorDeviceId) {
  return request({
    url: `/video-devices/${videoDeviceId}/bind`,
    method: 'post',
    params: { monitorDeviceId }
  })
}

/**
 * 解绑视频设备
 * @param {number} videoDeviceId - 视频设备ID
 * @returns {Promise}
 */
export function unbindVideoDevice(videoDeviceId) {
  return request({
    url: `/video-devices/${videoDeviceId}/unbind`,
    method: 'post'
  })
}

/**
 * 获取未绑定的视频设备列表
 * @returns {Promise}
 */
export function getUnboundVideoDevices() {
  return request({
    url: '/video-devices/unbound',
    method: 'get'
  })
}

/**
 * 根据监测设备ID获取绑定的视频设备
 * @param {number} monitorDeviceId - 监测设备ID
 * @returns {Promise}
 */
export function getVideoDeviceByMonitor(monitorDeviceId) {
  return request({
    url: `/video-devices/by-monitor/${monitorDeviceId}`,
    method: 'get'
  })
}

// ============ 兼容旧API的便捷函数 ============

/**
 * 获取设备的视频流URL（兼容旧接口）
 * 根据监测设备ID查找绑定的视频设备，然后返回视频流URL
 * @param {number} monitorDeviceId - 监测设备ID
 * @returns {Promise<{status: number, data: {streamUrl: string}, message: string}>}
 */
export function getDeviceVideoUrl(monitorDeviceId) {
  // 先获取绑定的视频设备
  return getVideoDeviceByMonitor(monitorDeviceId).then(res => {
    if (res.status === 200 && res.data) {
      // 再获取视频流URL
      return getVideoStreamUrl(res.data.id)
    } else {
      // 未绑定视频设备，返回空URL
      return { status: 404, data: { streamUrl: '' }, message: '未绑定视频设备' }
    }
  })
}

/**
 * 获取设备的视频快照（兼容旧接口）
 * @param {number} monitorDeviceId - 监测设备ID
 * @returns {Promise}
 */
export function getDeviceSnapshot(monitorDeviceId) {
  return getVideoDeviceByMonitor(monitorDeviceId).then(res => {
    if (res.status === 200 && res.data && res.data.snapshotUrl) {
      return { status: 200, data: res.data.snapshotUrl, message: '获取成功' }
    } else {
      return { status: 404, data: '', message: '未找到视频快照' }
    }
  })
}

/**
 * 获取多个设备的视频状态（兼容旧接口）
 * @param {Array<number>} monitorDeviceIds - 监测设备ID列表
 * @returns {Promise}
 */
export function getDevicesVideoStatus(monitorDeviceIds) {
  // 批量查询每个设备绑定的视频设备
  const promises = monitorDeviceIds.map(id => {
    return getVideoDeviceByMonitor(id).then(res => {
      return {
        deviceId: id,
        hasVideo: res.status === 200 && res.data !== null,
        videoDevice: res.data
      }
    }).catch(() => {
      return {
        deviceId: id,
        hasVideo: false,
        videoDevice: null
      }
    })
  })

  return Promise.all(promises).then(results => {
    return { status: 200, data: results, message: '获取成功' }
  })
}

// ============ 模拟数据（用于演示） ============

/**
 * 占位函数：返回模拟视频URL
 * 用于前端开发和演示
 * @param {number|string} deviceId - 设备ID
 * @returns {string} 模拟视频URL
 */
export function getMockVideoUrl(deviceId) {
  // 返回一个占位图片或视频
  return 'https://via.placeholder.com/640x360/1890ff/ffffff?text=Video+Stream+' + deviceId
}

/**
 * 占位函数：返回模拟缩略图
 * @param {number|string} deviceId - 设备ID
 * @returns {string} 模拟缩略图URL
 */
export function getMockSnapshotUrl(deviceId) {
  return 'https://via.placeholder.com/320x180/1890ff/ffffff?text=Device+' + deviceId
}
