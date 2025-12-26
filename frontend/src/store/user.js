import { defineStore } from 'pinia'
import { login as loginApi, getUserInfo, logout as logoutApi } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.userInfo?.role === 'ADMIN',
    userName: (state) => state.userInfo?.fullName || state.userInfo?.username || '',
    userRole: (state) => state.userInfo?.role || ''
  },

  actions: {
    // 登录
    async login(credentials) {
      try {
        const res = await loginApi(credentials)
        if (res.status === 200) {
          this.token = res.data.token
          this.userInfo = res.data.userInfo
          localStorage.setItem('token', res.data.token)
          localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))
          return res.data
        }
        throw new Error(res.message)
      } catch (error) {
        throw error
      }
    },

    // 获取用户信息
    async fetchUserInfo() {
      try {
        const res = await getUserInfo()
        if (res.status === 200) {
          this.userInfo = res.data
          localStorage.setItem('userInfo', JSON.stringify(res.data))
          return res.data
        }
        throw new Error(res.message)
      } catch (error) {
        throw error
      }
    },

    // 登出
    async logout() {
      try {
        await logoutApi()
      } catch (error) {
        console.error('登出请求失败:', error)
      } finally {
        this.token = ''
        this.userInfo = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')

        // 重置路由（清除动态加载的路由）
        const { resetRouter } = await import('@/router/index')
        resetRouter()
      }
    }
  }
})
