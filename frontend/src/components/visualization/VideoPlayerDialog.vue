<template>
  <el-dialog
    v-model="dialogVisible"
    :title="`设备视频 - ${device?.deviceName || ''}`"
    width="800px"
    :close-on-click-modal="false"
    @close="handleClose"
    class="video-dialog"
  >
    <div class="video-container">
      <!-- 视频播放区域（预留） -->
      <div class="video-player-area">
        <div class="placeholder-content">
          <el-icon :size="80" color="#1890ff">
            <VideoCamera />
          </el-icon>
          <p class="placeholder-text">视频播放器区域</p>
          <p class="placeholder-hint">
            设备：{{ device?.deviceName }} ({{ device?.deviceCode }})
          </p>
          <p class="placeholder-hint">状态：{{ device?.status === 'ONLINE' ? '在线' : '离线' }}</p>
          <el-alert
            type="info"
            :closable="false"
            show-icon
          >
            此功能需要后端提供视频流接口支持
          </el-alert>
        </div>
      </div>

      <!-- 设备信息 -->
      <div v-if="device" class="device-details">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="设备名称">
            {{ device.deviceName }}
          </el-descriptions-item>
          <el-descriptions-item label="设备编码">
            {{ device.deviceCode }}
          </el-descriptions-item>
          <el-descriptions-item label="设备类型">
            <el-tag :type="device.deviceType === 'RADIATION_MONITOR' ? 'danger' : 'success'" size="small">
              {{ device.deviceType === 'RADIATION_MONITOR' ? '辐射监测设备' : '环境监测设备' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="设备状态">
            <el-tag :type="device.status === 'ONLINE' ? 'success' : 'info'" size="small">
              {{ device.status === 'ONLINE' ? '在线' : '离线' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="安装位置" :span="2">
            {{ device.location || '未设置' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import { VideoCamera } from '@element-plus/icons-vue'
import { useVisualizationStore } from '@/store/visualization'

const visualizationStore = useVisualizationStore()

const dialogVisible = computed({
  get: () => visualizationStore.videoDialogVisible,
  set: (val) => {
    if (!val) {
      visualizationStore.closeVideoDialog()
    }
  }
})

const device = computed(() => visualizationStore.videoDevice)

const handleClose = () => {
  visualizationStore.closeVideoDialog()
}
</script>

<style scoped>
.video-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.video-container {
  display: flex;
  flex-direction: column;
}

.video-player-area {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #0a1929;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-content {
  text-align: center;
  color: #ffffff;
}

.placeholder-text {
  font-size: 18px;
  font-weight: 600;
  margin: 16px 0 8px;
}

.placeholder-hint {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  margin: 4px 0;
}

.device-details {
  padding: 20px;
}
</style>
