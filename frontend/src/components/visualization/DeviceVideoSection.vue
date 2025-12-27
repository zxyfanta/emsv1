<template>
  <div class="device-video-section">
    <div class="section-header">
      <h3 class="section-title">
        <el-icon><VideoCamera /></el-icon>
        设备视频 ({{ videoDevices.length }})
      </h3>
      <el-input
        v-model="searchQuery"
        placeholder="搜索视频设备名称/编码"
        prefix-icon="Search"
        size="small"
        clearable
        class="search-input"
      />
    </div>

    <el-scrollbar class="video-cards-container">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p>加载视频设备中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="filteredDevices.length === 0" class="empty-state">
        <el-empty :description="searchQuery ? '未找到匹配的视频设备' : '暂无视频设备'" />
      </div>

      <!-- 视频卡片网格 -->
      <div v-else class="video-cards-grid">
        <VideoCard
          v-for="videoDevice in filteredDevices"
          :key="videoDevice.id"
          :video-device="videoDevice"
          :linked-device="getLinkedDevice(videoDevice)"
          @click="handleVideoClick"
        />
      </div>
    </el-scrollbar>

    <!-- 视频播放弹窗 -->
    <VideoPlayerDialog />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { VideoCamera, Loading } from '@element-plus/icons-vue'
import VideoCard from './VideoCard.vue'
import VideoPlayerDialog from './VideoPlayerDialog.vue'
import { useVisualizationStore } from '@/store/visualization'
import { getAllVideoDevices } from '@/api/video'
import { ElMessage } from 'element-plus'

const props = defineProps({
  monitorDevices: {
    type: Array,
    default: () => []
  }
})

const visualizationStore = useVisualizationStore()
const searchQuery = ref('')
const loading = ref(false)
const videoDevices = ref([])

// 创建监测设备的ID映射，方便查找
const monitorDeviceMap = computed(() => {
  const map = new Map()
  props.monitorDevices.forEach(device => {
    map.set(device.id, device)
  })
  return map
})

// 根据linkedDeviceId获取绑定的监测设备信息
const getLinkedDevice = (videoDevice) => {
  if (!videoDevice.linkedDeviceId) {
    return null
  }
  return monitorDeviceMap.value.get(videoDevice.linkedDeviceId) || null
}

// 加载视频设备列表
const loadVideoDevices = async () => {
  loading.value = true
  try {
    const res = await getAllVideoDevices()
    if (res.status === 200) {
      videoDevices.value = res.data || []
    }
  } catch (error) {
    console.error('[加载视频设备] 失败:', error)
    ElMessage.error('加载视频设备失败')
  } finally {
    loading.value = false
  }
}

// 过滤视频设备（按名称或编码搜索）
const filteredDevices = computed(() => {
  if (!searchQuery.value) {
    return videoDevices.value
  }
  const query = searchQuery.value.toLowerCase()
  return videoDevices.filter(vd =>
    vd.deviceName?.toLowerCase().includes(query) ||
    vd.deviceCode?.toLowerCase().includes(query)
  )
})

// 点击设备卡片打开视频
const handleVideoClick = (videoDevice) => {
  visualizationStore.openVideoDialog({
    ...videoDevice,
    linkedDevice: getLinkedDevice(videoDevice)
  })
}

// 组件挂载时加载视频设备
onMounted(() => {
  loadVideoDevices()
})

// 监听监测设备列表变化，重新加载视频设备
watch(() => props.monitorDevices, () => {
  loadVideoDevices()
}, { deep: true })
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
  width: 200px;
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
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: rgba(255, 255, 255, 0.6);
}

.loading-state p {
  margin-top: 12px;
  font-size: 14px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}
</style>
