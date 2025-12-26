import request from '@/utils/request'

// 获取企业列表
export const getCompanyList = (params) => {
  return request({
    url: '/companies',
    method: 'get',
    params
  })
}

// 获取企业详情
export const getCompanyDetail = (id) => {
  return request({
    url: `/companies/${id}`,
    method: 'get'
  })
}

// 获取当前企业信息
export const getCurrentCompany = () => {
  return request({
    url: '/companies/current',
    method: 'get'
  })
}

// 获取当前企业信息(别名,用于组件导入)
export const getCurrentCompanyInfo = getCurrentCompany

// 更新当前企业信息
export const updateCurrentCompany = (data) => {
  return request({
    url: '/companies/current',
    method: 'put',
    data
  })
}

// 创建企业
export const createCompany = (data) => {
  return request({
    url: '/companies',
    method: 'post',
    data
  })
}

// 更新企业
export const updateCompany = (id, data) => {
  return request({
    url: `/companies/${id}`,
    method: 'put',
    data
  })
}

// 删除企业
export const deleteCompany = (id) => {
  return request({
    url: `/companies/${id}`,
    method: 'delete'
  })
}

// 更新企业状态
export const updateCompanyStatus = (id, status) => {
  return request({
    url: `/companies/${id}/status`,
    method: 'patch',
    params: { status }
  })
}

// 搜索企业
export const searchCompany = (keyword) => {
  return request({
    url: '/companies/search',
    method: 'get',
    params: { keyword }
  })
}
