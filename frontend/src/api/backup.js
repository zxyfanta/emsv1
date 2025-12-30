import request from '@/utils/request'

// 备份API
export default {
  // 立即执行全量备份
  executeFullBackup() {
    return request({
      url: '/backup/execute/all',
      method: 'post'
    })
  },

  // 备份时序数据
  executeTimeseriesBackup() {
    return request({
      url: '/backup/execute/timeseries',
      method: 'post'
    })
  },

  // 备份业务数据
  executeBusinessBackup() {
    return request({
      url: '/backup/execute/business',
      method: 'post'
    })
  },

  // 备份系统配置
  executeSystemBackup() {
    return request({
      url: '/backup/execute/system',
      method: 'post'
    })
  },

  // 获取备份日志列表
  getBackupLogs(params) {
    return request({
      url: '/backup/logs',
      method: 'get',
      params
    })
  },

  // 获取最近备份记录
  getRecentBackups() {
    return request({
      url: '/backup/logs/recent',
      method: 'get'
    })
  },

  // 获取备份统计
  getStatistics() {
    return request({
      url: '/backup/statistics',
      method: 'get'
    })
  }
}
