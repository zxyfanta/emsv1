import request from '@/utils/request'

// 用户登录
export const login = (data) => {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

// 获取当前用户信息
export const getUserInfo = () => {
  return request({
    url: '/auth/me',
    method: 'get'
  })
}

// 刷新Token
export const refreshToken = () => {
  return request({
    url: '/auth/refresh',
    method: 'post'
  })
}

// 用户登出
export const logout = () => {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

// 验证Token
export const validateToken = () => {
  return request({
    url: '/auth/validate',
    method: 'get'
  })
}
