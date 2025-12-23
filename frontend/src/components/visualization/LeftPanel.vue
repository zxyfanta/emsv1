<template>
  <div class="left-panel-content">
    <!-- 设备统计卡片 -->
    <div class="stats-section">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-label">总设备数</div>
          <div class="stat-value">{{ devices.length }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">已定位</div>
          <div class="stat-value located">{{ devicesWithPosition.length }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">在线</div>
          <div class="stat-value online">{{ onlineCount }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">离线</div>
          <div class="stat-value offline">{{ offlineCount }}</div>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-section">
      <el-input
        v-model="searchQuery"
        placeholder="搜索在线设备"
        prefix-icon="Search"
        clearable
        size="small"
      />
    </div>

    <!-- 在线设备列表 -->
    <el-scrollbar class="device-list-section">
      <div v-if="filteredOnlineDevices.length === 0" class="empty-state">
        <el-empty description="暂无在线设备" :image-size="60" />
      </div>
      <div v-else class="device-list">
        <div
          v-for="device in filteredOnlineDevices"
          :key="device.id"
          class="device-item"
          @click="handleDeviceClick(device)"
        >
          <div class="device-info">
            <div class="device-name">{{ device.deviceName }}</div>
            <div class="device-code">{{ device.deviceCode }}</div>
          </div>
          <el-tag
            :type="device.deviceType === 'RADIATION_MONITOR' ? 'danger' : 'success'"
            size="small"
          >
            {{ device.deviceType === 'RADIATION_MONITOR' ? '辐射' : '环境' }}
          </el-tag>
        </div>
      </div>
    </el-scrollbar>

    <!-- 返回按钮（固定底部） -->
    <div class="back-button-section">
      <el-button type="primary" @click="$emit('back')" style="width: 100%;">
        <el-icon><Back /></el-icon>
        返回系统
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Back } from '@element-plus/icons-vue'
import { useVisualizationStore } from '@/store/visualization'

const props = defineProps({
  devices: {
    type: Array,
    required: true
  },
  onlineCount: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['back'])

const visualizationStore = useVisualizationStore()
const searchQuery = ref('')

const devicesWithPosition = computed(() => {
  return props.devices.filter(d => d.positionX !== null && d.positionY !== null)
})

const offlineCount = computed(() => {
  return props.devices.filter(d => d.status !== 'ONLINE').length
})

// 获取所有在线设备
const onlineDevices = computed(() => {
  return props.devices.filter(d => d.status === 'ONLINE')
})

// 过滤在线设备（按名称或编码搜索）
const filteredOnlineDevices = computed(() => {
  if (!searchQuery.value) {
    return onlineDevices.value
  }
  const query = searchQuery.value.toLowerCase()
  return onlineDevices.value.filter(device =>
    device.deviceName?.toLowerCase().includes(query) ||
    device.deviceCode?.toLowerCase().includes(query)
  )
})

// 点击设备
const handleDeviceClick = (device) => {
  // 同时选中设备并同步到右侧实时信息栏
  visualizationStore.setSelectedDevice(device)
}
</script>

<style scoped>
.left-panel-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 50px 16px 16px 16px; /* 顶部留出panel-header空间 */
}

.stats-section {
  flex-shrink: 0;
  margin-bottom: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.stat-card {
  background: rgba(24, 144, 255, 0.08);
  border: 1px solid rgba(24, 144, 255, 0.2);
  border-radius: 8px;
  padding: 10px;
  text-align: center;
}

.stat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 6px;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #ffffff;
}

.stat-value.located {
  color: #1890ff;
}

.stat-value.online {
  color: #52c41a;
}

.stat-value.offline {
  color: #8c8c8c;
}

.search-section {
  flex-shrink: 0;
  margin-bottom: 12px;
}

.search-section :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(24, 144, 255, 0.3);
}

.search-section :deep(.el-input__inner) {
  color: #ffffff;
}

.device-list-section {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 12px;
}

.device-list-section :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

.device-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.device-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(24, 144, 255, 0.1);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
}

.device-item:hover {
  background: rgba(24, 144, 255, 0.1);
  border-color: rgba(24, 144, 255, 0.4);
  transform: translateX(4px);
}

.device-info {
  flex: 1;
}

.device-name {
  font-size: 13px;
  font-weight: 500;
  color: #ffffff;
  margin-bottom: 2px;
}

.device-code {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.back-button-section {
  flex-shrink: 0;
  height: 60px;
  display: flex;
  align-items: center;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}
</style>
