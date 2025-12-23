<template>
  <div class="right-panel-content">
    <!-- 设备筛选 -->
    <div class="filter-section">
      <div class="section-title">
        <Decoration3 style="width:100%; height:20px;" />
        <span>设备列表</span>
      </div>
      <div class="filter-buttons">
        <el-button
          :type="filterType === 'all' ? 'primary' : 'info'"
          size="small"
          @click="filterType = 'all'"
        >
          全部
        </el-button>
        <el-button
          :type="filterType === 'RADIATION_MONITOR' ? 'danger' : 'info'"
          size="small"
          @click="filterType = 'RADIATION_MONITOR'"
        >
          辐射
        </el-button>
        <el-button
          :type="filterType === 'ENVIRONMENT_STATION' ? 'success' : 'info'"
          size="small"
          @click="filterType = 'ENVIRONMENT_STATION'"
        >
          环境
        </el-button>
      </div>
    </div>

    <!-- 设备列表 -->
    <div class="device-list">
      <div
        v-for="device in filteredDevices"
        :key="device.id"
        class="device-item"
        :class="{
          'active': selectedDevice?.id === device.id,
          'radiation': device.deviceType === 'RADIATION_MONITOR',
          'offline': device.status !== 'ONLINE'
        }"
        @click="handleDeviceClick(device)"
      >
        <div class="device-icon">
          <div class="status-dot" :class="{ online: device.status === 'ONLINE' }"></div>
        </div>
        <div class="device-info">
          <div class="device-name">{{ device.deviceName }}</div>
          <div class="device-code">{{ device.deviceCode }}</div>
        </div>
        <div class="device-type-badge">
          <el-tag
            :type="device.deviceType === 'RADIATION_MONITOR' ? 'danger' : 'success'"
            size="small"
          >
            {{ device.deviceType === 'RADIATION_MONITOR' ? '辐射' : '环境' }}
          </el-tag>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="filteredDevices.length === 0" class="empty-state">
        <div class="empty-text">暂无设备数据</div>
      </div>
    </div>

    <!-- 实时数据占位 -->
    <div class="realtime-section">
      <div class="section-title">
        <Decoration4 style="width:5px; height:20px;" />
        <span>实时数据</span>
      </div>
      <div class="realtime-placeholder">
        <div class="placeholder-text">点击设备查看实时数据</div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="actions-section">
      <div class="section-title">
        <Decoration7 style="width:150px; height:30px;">快捷操作</Decoration7>
      </div>
      <div class="action-buttons">
        <el-button type="primary" size="small" @click="handleRefresh">
          刷新数据
        </el-button>
        <el-button size="small" @click="handleExport">
          导出报表
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  devices: {
    type: Array,
    required: true
  },
  selectedDevice: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['device-click', 'edit-device', 'view-data', 'refresh'])

const filterType = ref('all')

const filteredDevices = computed(() => {
  if (filterType.value === 'all') {
    return props.devices
  }
  return props.devices.filter(d => d.deviceType === filterType.value)
})

const handleDeviceClick = (device) => {
  emit('device-click', device)
}

const handleRefresh = () => {
  ElMessage.info('数据刷新功能开发中')
  emit('refresh')
}

const handleExport = () => {
  ElMessage.info('报表导出功能开发中')
  emit('export')
}
</script>

<style scoped>
.right-panel-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  height: 100%;
  overflow: hidden;
}

.filter-section {
  flex-shrink: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: bold;
  color: #fff;
}

.filter-buttons {
  display: flex;
  gap: 8px;
}

.device-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
}

.device-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  border-left: 3px solid transparent;
}

.device-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.device-item.active {
  background: rgba(66, 211, 146, 0.2);
  border-left-color: #42d392;
}

.device-item.radiation.active {
  background: rgba(255, 68, 68, 0.2);
  border-left-color: #ff4444;
}

.device-item.offline {
  opacity: 0.6;
}

.device-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #909399;
}

.status-dot.online {
  background: #67c23a;
  box-shadow: 0 0 8px #67c23a;
}

.device-info {
  flex: 1;
  min-width: 0;
}

.device-name {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.device-code {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.device-type-badge {
  flex-shrink: 0;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.empty-text {
  color: #909399;
  font-size: 14px;
}

.realtime-section {
  flex-shrink: 0;
}

.realtime-placeholder {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.placeholder-text {
  color: #909399;
  font-size: 12px;
}

.actions-section {
  flex-shrink: 0;
}

.action-buttons {
  display: flex;
  gap: 8px;
}
</style>
