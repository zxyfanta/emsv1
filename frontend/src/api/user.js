import request from '@/utils/request'

// 获取用户列表
export const getUserList = (params) => {
  return request({
    url: '/users',
    method: 'get',
    params
  })
}

// 获取用户详情
export const getUserDetail = (id) => {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

// 创建用户
export const createUser = (data) => {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

// 更新用户
export const updateUser = (id, data) => {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

// 删除用户
export const deleteUser = (id) => {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}

// 重置用户密码
export const resetUserPassword = (id, newPassword) => {
  return request({
    url: `/users/${id}/password`,
    method: 'put',
    data: { password: newPassword }
  })
}

// 修改当前用户密码
export const changePassword = (data) => {
  return request({
    url: '/users/change-password',
    method: 'post',
    data
  })
}

// 获取当前用户信息
export const getCurrentUser = () => {
  return request({
    url: '/users/current',
    method: 'get'
  })
}

// 更新当前用户个人信息
export const updateCurrentProfile = (data) => {
  return request({
    url: '/users/current',
    method: 'put',
    data
  })
}
