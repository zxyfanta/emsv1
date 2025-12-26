<template>
  <div class="realtime-info-section">
    <div class="section-header">
      <h3 class="section-title">
        <el-icon><DataLine /></el-icon>
        实时信息
      </h3>
      <div v-if="selectedDevice" class="device-indicator">
        <span class="device-name">{{ selectedDevice.deviceName }}</span>
        <el-tag
          :type="selectedDevice.deviceType === 'RADIATION_MONITOR' ? 'danger' : 'success'"
          size="small"
        >
          {{ selectedDevice.deviceType === 'RADIATION_MONITOR' ? '辐射' : '环境' }}
        </el-tag>
      </div>
    </div>

    <!-- 数据显示区域 -->
    <div class="data-display-area">
      <div v-if="!selectedDevice" class="no-selection">
        <el-icon :size="48"><Select /></el-icon>
        <p>请选择设备查看实时数据</p>
      </div>
      <div v-else class="data-content">
        <!-- 设备基本信息 -->
        <div class="device-header">
          <div class="device-title">
            <h4>{{ selectedDevice.deviceName }}</h4>
            <el-tag :type="selectedDevice.status === 'ONLINE' ? 'success' : 'info'" size="small">
              {{ selectedDevice.status === 'ONLINE' ? '在线' : '离线' }}
            </el-tag>
          </div>
          <div class="device-code">{{ selectedDevice.deviceCode }}</div>
        </div>

        <!-- CPM 数据展示 -->
        <div class="cpm-display">
          <div class="cpm-label">当前 CPM</div>
          <div :class="['cpm-value', { loading: loading }]">
            {{ loading ? '--' : (latestData?.cpm || '无数据') }}
          </div>
          <div class="cpm-unit">CPM</div>
        </div>

        <!-- 其他数据 -->
        <div v-if="latestData && !loading" class="extra-data">
          <div class="data-row">
            <span class="label">更新时间:</span>
            <span class="value">{{ formatTime(latestData.recordTime) }}</span>
          </div>
          <div v-if="latestData.batVolt !== undefined" class="data-row">
            <span class="label">电池电压:</span>
            <span class="value">{{ latestData.batVolt?.toFixed(2) || '--' }} V</span>
          </div>
          <div v-if="latestData.temperature !== undefined" class="data-row">
            <span class="label">温度:</span>
            <span class="value">{{ latestData.temperature?.toFixed(1) || '--' }} °C</span>
          </div>
          <div v-if="latestData.wetness !== undefined" class="data-row">
            <span class="label">湿度:</span>
            <span class="value">{{ latestData.wetness?.toFixed(1) || '--' }} %</span>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-hint">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在获取实时数据...</span>
        </div>

        <!-- 离线提示 -->
        <el-alert
          v-if="selectedDevice.status === 'OFFLINE'"
          type="warning"
          :closable="false"
          show-icon
        >
          设备离线，无法获取实时数据
        </el-alert>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { DataLine, Select, Loading } from '@element-plus/icons-vue'
import { useVisualizationStore } from '@/store/visualization'
import { getRadiationDeviceLatestData, getEnvironmentDeviceLatestData } from '@/api/device'
import { sseManager } from '@/utils/sse'

const props = defineProps({
  devices: {
    type: Array,
    default: () => []
  }
})

const visualizationStore = useVisualizationStore()
const loading = ref(false)
const latestData = ref(null)
let unsubscribe = null // SSE取消订阅函数

// 选中的设备（从 store 获取）
const selectedDevice = computed(() => visualizationStore.selectedDevice)

// 获取设备最新数据（初始加载时使用）
const fetchLatestData = async () => {
  if (!selectedDevice.value || selectedDevice.value.status === 'OFFLINE') {
    return
  }

  loading.value = true
  try {
    let response
    if (selectedDevice.value.deviceType === 'RADIATION_MONITOR') {
      response = await getRadiationDeviceLatestData(selectedDevice.value.deviceCode)
    } else {
      response = await getEnvironmentDeviceLatestData(selectedDevice.value.deviceCode)
    }

    if (response.status === 200) {
      // 后端返回的字段名已经是小写，直接使用
      latestData.value = response.data
      // 缓存数据
      visualizationStore.cacheRealtimeData(selectedDevice.value.deviceCode, latestData.value)
    }
  } catch (error) {
    console.error('获取实时数据失败:', error)
    // 尝试使用缓存数据
    const cached = visualizationStore.getCachedRealtimeData(selectedDevice.value.deviceCode)
    if (cached) {
      latestData.value = cached
    }
  } finally {
    loading.value = false
  }
}

// 处理SSE推送的数据
// SSE数据结构已扁平化，数据直接在顶层，无需二次解析
const handleSSEMessage = (eventType, data) => {
  if (!selectedDevice.value) return

  // 检查数据是否属于当前选中的设备
  if (data.deviceCode !== selectedDevice.value.deviceCode) {
    return
  }

  // 数据已扁平化，直接使用
  try {
    // 根据事件类型更新数据
    if (eventType === 'radiation-data') {
      latestData.value = {
        cpm: data.cpm,
        batVolt: data.batVolt,
        recordTime: data.recordTime || data.timestamp
      }
    } else if (eventType === 'environment-data') {
      latestData.value = {
        cpm: data.cpm,
        temperature: data.temperature,
        wetness: data.wetness,
        windspeed: data.windspeed,
        recordTime: data.recordTime || data.timestamp
      }
    }

    // 缓存数据
    if (latestData.value) {
      visualizationStore.cacheRealtimeData(selectedDevice.value.deviceCode, latestData.value)
    }

    console.log('[RealtimeInfoSection] SSE数据已更新:', latestData.value)
  } catch (e) {
    console.error('[RealtimeInfoSection] 解析SSE数据失败:', e)
  }
}

// 启动SSE监听
const startSSE = () => {
  // 取消之前的订阅
  if (unsubscribe) {
    unsubscribe()
  }

  // 确保SSE已初始化
  const status = sseManager.getStatus()
  if (!status.connected) {
    console.warn('[RealtimeInfoSection] SSE未连接，尝试初始化...')
    sseManager.init()
  }

  // 订阅辐射数据和环境数据
  const unsubRadiation = sseManager.subscribe('radiation-data', (data) => {
    handleSSEMessage('radiation-data', data)
  })

  const unsubEnvironment = sseManager.subscribe('environment-data', (data) => {
    handleSSEMessage('environment-data', data)
  })

  // 保存取消订阅函数
  unsubscribe = () => {
    unsubRadiation()
    unsubEnvironment()
  }

  // 先通过REST API获取初始数据
  fetchLatestData()
}

// 停止SSE监听
const stopSSE = () => {
  if (unsubscribe) {
    unsubscribe()
    unsubscribe = null
  }
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return '--'
  try {
    const date = new Date(timeStr)
    return date.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    })
  } catch {
    return timeStr
  }
}

// 监听选中设备变化
watch(() => visualizationStore.selectedDeviceId, () => {
  latestData.value = null
  startSSE()
}, { immediate: true })

// 组件挂载时默认选择第一个在线设备
onMounted(() => {
  if (!selectedDevice.value && props.devices.length > 0) {
    visualizationStore.setSelectedDevice(props.devices[0])
  }
})

onBeforeUnmount(() => {
  stopSSE()
})
</script>

<style scoped>
.realtime-info-section {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
}

.device-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.device-indicator .device-name {
  font-size: 13px;
  color: #ffffff;
  font-weight: 500;
}

.data-display-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.no-selection {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: rgba(255, 255, 255, 0.4);
}

.no-selection p {
  margin-top: 12px;
  font-size: 14px;
}

.data-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.device-header {
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(24, 144, 255, 0.2);
}

.device-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.device-title h4 {
  margin: 0;
  font-size: 16px;
  color: #ffffff;
}

.device-code {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.cpm-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.15) 0%, rgba(24, 144, 255, 0.05) 100%);
  border: 1px solid rgba(24, 144, 255, 0.3);
  border-radius: 8px;
}

.cpm-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 8px;
}

.cpm-value {
  font-size: 48px;
  font-weight: 700;
  color: #1890ff;
  line-height: 1;
  margin-bottom: 4px;
}

.cpm-value.loading {
  opacity: 0.5;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.5; }
  50% { opacity: 1; }
}

.cpm-unit {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.extra-data {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.data-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 4px;
}

.data-row .label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.data-row .value {
  font-size: 13px;
  color: #ffffff;
  font-weight: 500;
}

.loading-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 120px;
}
</style>
