<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409eff">
              <el-icon :size="32"><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalDevices }}</div>
              <div class="stat-label">设备总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67c23a">
              <el-icon :size="32"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.onlineDevices }}</div>
              <div class="stat-label">在线设备</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #e6a23c">
              <el-icon :size="32"><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.offlineDevices }}</div>
              <div class="stat-label">离线设备</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #f56c6c">
              <el-icon :size="32"><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.faultDevices }}</div>
              <div class="stat-label">故障设备</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>设备类型分布</span>
          </template>
          <div ref="deviceTypeChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>设备状态分布</span>
          </template>
          <div ref="deviceStatusChartRef" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getDeviceList } from '@/api/device'

const deviceTypeChartRef = ref()
const deviceStatusChartRef = ref()
let deviceTypeChart = null
let deviceStatusChart = null

const statistics = ref({
  totalDevices: 0,
  onlineDevices: 0,
  offlineDevices: 0,
  faultDevices: 0
})

const loadStatistics = async () => {
  try {
    const res = await getDeviceList({ page: 0, size: 1000 })
    if (res.status === 200) {
      const devices = res.data.content
      statistics.value.totalDevices = devices.length
      statistics.value.onlineDevices = devices.filter(d => d.status === 'ONLINE').length
      statistics.value.offlineDevices = devices.filter(d => d.status === 'OFFLINE').length
      statistics.value.faultDevices = devices.filter(d => d.status === 'FAULT').length

      initCharts(devices)
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

const initCharts = (devices) => {
  // 设备类型分布
  const typeData = devices.reduce((acc, device) => {
    const type = device.deviceType === 'RADIATION_MONITOR' ? '辐射设备' : '环境设备'
    acc[type] = (acc[type] || 0) + 1
    return acc
  }, {})

  deviceTypeChart = echarts.init(deviceTypeChartRef.value)
  deviceTypeChart.setOption({
    tooltip: {
      trigger: 'item'
    },
    series: [{
      type: 'pie',
      radius: '60%',
      data: Object.entries(typeData).map(([name, value]) => ({ name, value })),
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  })

  // 设备状态分布
  const statusData = devices.reduce((acc, device) => {
    const statusMap = {
      'ONLINE': '在线',
      'OFFLINE': '离线',
      'MAINTENANCE': '维护',
      'FAULT': '故障'
    }
    const status = statusMap[device.status] || device.status
    acc[status] = (acc[status] || 0) + 1
    return acc
  }, {})

  deviceStatusChart = echarts.init(deviceStatusChartRef.value)
  deviceStatusChart.setOption({
    tooltip: {
      trigger: 'item'
    },
    series: [{
      type: 'pie',
      radius: ['40%', '60%'],
      data: Object.entries(statusData).map(([name, value]) => ({ name, value })),
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  })
}

const handleResize = () => {
  deviceTypeChart?.resize()
  deviceStatusChart?.resize()
}

onMounted(() => {
  loadStatistics()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  deviceTypeChart?.dispose()
  deviceStatusChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}
</style>
