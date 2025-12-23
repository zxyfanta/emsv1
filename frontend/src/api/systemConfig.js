import request from '@/utils/request'

/**
 * 获取系统配置
 * 返回当前启用的功能模块（辐射监测、环境监测）
 */
export const getSystemConfig = () => {
  return request({
    url: '/system-config',
    method: 'get'
  })
}
