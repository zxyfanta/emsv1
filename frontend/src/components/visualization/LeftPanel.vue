<template>
  <div class="left-panel-content">
    <!-- 设备统计卡片 -->
    <div class="stats-section">
      <Decoration1 :color="['#42d392', '#647eff']" style="width:100%; height:30px;" />
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

    <!-- 设备类型分布 -->
    <div class="chart-section">
      <div class="section-title">
        <Decoration6 style="width:100%; height:20px;" />
        <span>设备类型分布</span>
      </div>
      <div ref="typeChartRef" class="chart-container"></div>
    </div>

    <!-- 设备状态统计 -->
    <div class="chart-section">
      <div class="section-title">
        <Decoration6 :reverse="true" style="width:100%; height:20px;" />
        <span>设备状态统计</span>
      </div>
      <div ref="statusChartRef" class="chart-container"></div>
    </div>

    <!-- 告警信息占位 -->
    <div class="alert-section">
      <div class="section-title">
        <Decoration5 style="width:100%; height:30px;" />
        <span>告警信息</span>
      </div>
      <div class="alert-list">
        <div class="alert-placeholder">暂无告警信息</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'

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

const emit = defineEmits(['device-click'])

const typeChartRef = ref(null)
const statusChartRef = ref(null)
let typeChart = null
let statusChart = null

const devicesWithPosition = computed(() => {
  return props.devices.filter(d => d.positionX !== null && d.positionY !== null)
})

const offlineCount = computed(() => {
  return props.devices.filter(d => d.status !== 'ONLINE').length
})

// 初始化类型分布饼图
const initTypeChart = () => {
  if (!typeChartRef.value) return

  if (!typeChart) {
    typeChart = echarts.init(typeChartRef.value)
  }

  const radiationCount = props.devices.filter(d => d.deviceType === 'RADIATION_MONITOR').length
  const environmentCount = props.devices.filter(d => d.deviceType === 'ENVIRONMENT_STATION').length

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      textStyle: { color: '#fff' }
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['50%', '50%'],
      data: [
        { value: radiationCount, name: '辐射设备', itemStyle: { color: '#ff4444' } },
        { value: environmentCount, name: '环境设备', itemStyle: { color: '#44cc44' } }
      ],
      label: {
        color: '#fff',
        fontSize: 12
      },
      labelLine: {
        lineStyle: { color: '#fff' }
      }
    }]
  }

  typeChart.setOption(option)
}

// 初始化状态统计柱状图
const initStatusChart = () => {
  if (!statusChartRef.value) return

  if (!statusChart) {
    statusChart = echarts.init(statusChartRef.value)
  }

  const radiationOnline = props.devices.filter(d =>
    d.deviceType === 'RADIATION_MONITOR' && d.status === 'ONLINE'
  ).length
  const radiationOffline = props.devices.filter(d =>
    d.deviceType === 'RADIATION_MONITOR' && d.status !== 'ONLINE'
  ).length
  const envOnline = props.devices.filter(d =>
    d.deviceType === 'ENVIRONMENT_STATION' && d.status === 'ONLINE'
  ).length
  const envOffline = props.devices.filter(d =>
    d.deviceType === 'ENVIRONMENT_STATION' && d.status !== 'ONLINE'
  ).length

  const option = {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      textStyle: { color: '#fff' }
    },
    grid: {
      left: '10%',
      right: '10%',
      top: '10%',
      bottom: '20%'
    },
    xAxis: {
      type: 'category',
      data: ['辐射设备', '环境设备'],
      axisLabel: { color: '#fff' },
      axisLine: { lineStyle: { color: '#333' } }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#fff' },
      axisLine: { lineStyle: { color: '#333' } },
      splitLine: { lineStyle: { color: '#333', type: 'dashed' } }
    },
    series: [
      {
        name: '在线',
        type: 'bar',
        stack: 'status',
        data: [radiationOnline, envOnline],
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '离线',
        type: 'bar',
        stack: 'status',
        data: [radiationOffline, envOffline],
        itemStyle: { color: '#909399' }
      }
    ],
    legend: {
      textStyle: { color: '#fff' },
      bottom: 0
    }
  }

  statusChart.setOption(option)
}

const handleResize = () => {
  typeChart?.resize()
  statusChart?.resize()
}

onMounted(() => {
  initTypeChart()
  initStatusChart()

  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  typeChart?.dispose()
  statusChart?.dispose()
})

// 监听设备数据变化，更新图表
watch(() => props.devices, () => {
  initTypeChart()
  initStatusChart()
}, { deep: true })
</script>

<style scoped>
.left-panel-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  height: 100%;
  overflow-y: auto;
}

.stats-section {
  flex-shrink: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 10px;
}

.stat-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px;
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.stat-value.located {
  color: #409eff;
}

.stat-value.online {
  color: #67c23a;
}

.stat-value.offline {
  color: #909399;
}

.chart-section {
  flex: 1;
  min-height: 200px;
  display: flex;
  flex-direction: column;
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

.chart-container {
  flex: 1;
  min-height: 150px;
}

.alert-section {
  flex-shrink: 0;
}

.alert-list {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 12px;
  min-height: 80px;
}

.alert-placeholder {
  text-align: center;
  color: #909399;
  font-size: 12px;
  padding: 20px 0;
}
</style>
