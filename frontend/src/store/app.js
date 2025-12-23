import { defineStore } from 'pinia'
import { getSystemConfig } from '@/api/systemConfig'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: false,
    isLoading: false,
    isFullscreen: false,
    // 系统功能开关配置
    systemConfig: {
      radiationEnabled: true,
      environmentEnabled: true
    }
  }),

  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
    },

    setLoading(loading) {
      this.isLoading = loading
    },

    setFullscreen(fullscreen) {
      this.isFullscreen = fullscreen
    },

    /**
     * 获取系统配置
     * 从后端获取当前启用的功能模块
     */
    async fetchSystemConfig() {
      try {
        const res = await getSystemConfig()
        if (res.status === 200 && res.data) {
          this.systemConfig = res.data
        }
      } catch (error) {
        console.error('获取系统配置失败:', error)
        // 保持默认值
      }
    }
  }
})
