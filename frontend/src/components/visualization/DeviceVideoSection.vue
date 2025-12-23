<template>
  <div class="device-video-section">
    <div class="section-header">
      <h3 class="section-title">
        <el-icon><VideoCamera /></el-icon>
        设备视频
      </h3>
      <el-input
        v-model="searchQuery"
        placeholder="搜索设备名称/编码"
        prefix-icon="Search"
        size="small"
        clearable
        class="search-input"
      />
    </div>

    <el-scrollbar class="video-cards-container">
      <div v-if="filteredDevices.length === 0" class="empty-state">
        <el-empty description="暂无设备" />
      </div>
      <div v-else class="video-cards-grid">
        <VideoCard
          v-for="device in filteredDevices"
          :key="device.id"
          :device="device"
          @click="handleVideoClick"
        />
      </div>
    </el-scrollbar>

    <!-- 视频播放弹窗 -->
    <VideoPlayerDialog />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { VideoCamera } from '@element-plus/icons-vue'
import VideoCard from './VideoCard.vue'
import VideoPlayerDialog from './VideoPlayerDialog.vue'
import { useVisualizationStore } from '@/store/visualization'

const props = defineProps({
  devices: {
    type: Array,
    default: () => []
  }
})

const visualizationStore = useVisualizationStore()
const searchQuery = ref('')

// 过滤设备（按名称或编码搜索）
const filteredDevices = computed(() => {
  if (!searchQuery.value) {
    return props.devices
  }
  const query = searchQuery.value.toLowerCase()
  return props.devices.filter(device =>
    device.deviceName?.toLowerCase().includes(query) ||
    device.deviceCode?.toLowerCase().includes(query)
  )
})

// 点击设备卡片打开视频
const handleVideoClick = (device) => {
  visualizationStore.openVideoDialog(device)
}
</script>

<style scoped>
.device-video-section {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(24, 144, 255, 0.1);
  border-bottom: 1px solid rgba(24, 144, 255, 0.2);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1890ff;
  flex: 1;
}

.search-input {
  width: 160px;
}

.search-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(24, 144, 255, 0.3);
}

.search-input :deep(.el-input__inner) {
  color: #ffffff;
}

.video-cards-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.video-cards-container :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

.video-cards-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}
</style>
