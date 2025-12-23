import { defineStore } from 'pinia'

/**
 * 可视化面板状态管理
 * 用于管理可视化大屏中的选中设备、实时数据等状态
 */
export const useVisualizationStore = defineStore('visualization', {
  state: () => ({
    // 选中的设备（用于实时信息显示）
    selectedDevice: null,
    // 视频弹窗显示状态
    videoDialogVisible: false,
    // 当前播放视频的设备
    videoDevice: null,
    // 实时数据缓存
    realtimeDataCache: new Map()
  }),

  getters: {
    // 获取选中设备的ID
    selectedDeviceId: (state) => state.selectedDevice?.id || null,
    // 是否有选中设备
    hasSelectedDevice: (state) => !!state.selectedDevice
  },

  actions: {
    // 设置选中设备
    setSelectedDevice(device) {
      this.selectedDevice = device
    },

    // 打开视频弹窗
    openVideoDialog(device) {
      this.videoDevice = device
      this.videoDialogVisible = true
    },

    // 关闭视频弹窗
    closeVideoDialog() {
      this.videoDialogVisible = false
      this.videoDevice = null
    },

    // 缓存实时数据
    cacheRealtimeData(deviceId, data) {
      this.realtimeDataCache.set(deviceId, {
        data,
        timestamp: Date.now()
      })
    },

    // 获取缓存的实时数据
    getCachedRealtimeData(deviceId) {
      const cached = this.realtimeDataCache.get(deviceId)
      // 5秒内的缓存视为有效
      if (cached && Date.now() - cached.timestamp < 5000) {
        return cached.data
      }
      return null
    },

    // 清除过期的缓存数据
    clearExpiredCache() {
      const now = Date.now()
      for (const [deviceId, cached] of this.realtimeDataCache.entries()) {
        if (now - cached.timestamp >= 5000) {
          this.realtimeDataCache.delete(deviceId)
        }
      }
    }
  }
})
