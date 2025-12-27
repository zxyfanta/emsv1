<template>
  <div class="device-detail-panel">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="设备编码">
        {{ device.deviceCode }}
      </el-descriptions-item>
      <el-descriptions-item label="设备名称">
        {{ device.deviceName }}
      </el-descriptions-item>
      <el-descriptions-item label="设备类型">
        <el-tag :type="deviceTypeTagType">
          {{ deviceTypeLabel }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="设备状态">
        <el-tag :type="statusTagType">
          {{ statusLabel }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="设备位置">
        {{ device.location || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="可视化坐标">
        X: {{ device.positionX ?? '-' }}, Y: {{ device.positionY ?? '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="设备描述">
        {{ device.description || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="创建时间">
        {{ formatDate(device.createdAt) }}
      </el-descriptions-item>
      <el-descriptions-item label="更新时间">
        {{ formatDate(device.updatedAt) }}
      </el-descriptions-item>
    </el-descriptions>

    <div class="actions">
      <el-button type="primary" @click="handleEdit">编辑设备</el-button>
      <el-button @click="handleViewData">查看数据</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  device: {
    type: Object,
    required: true
  }
})

const router = useRouter()

const deviceTypeLabel = computed(() => {
  return props.device.deviceType === 'RADIATION_MONITOR' ? '辐射监测仪' : '环境监测站'
})

const deviceTypeTagType = computed(() => {
  return props.device.deviceType === 'RADIATION_MONITOR' ? 'danger' : 'success'
})

const statusLabel = computed(() => {
  const statusMap = {
    'ONLINE': '在线',
    'OFFLINE': '离线',
    'MAINTENANCE': '维护中',
    'FAULT': '故障'
  }
  return statusMap[props.device.status] || props.device.status
})

const statusTagType = computed(() => {
  const typeMap = {
    'ONLINE': 'success',
    'OFFLINE': 'info',
    'MAINTENANCE': 'warning',
    'FAULT': 'danger'
  }
  return typeMap[props.device.status] || 'info'
})

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const handleEdit = () => {
  router.push(`/devices/${props.device.id}/edit`)
}

const handleViewData = () => {
  if (props.device.deviceType === 'RADIATION_MONITOR') {
    router.push('/radiation-data')
  } else {
    router.push('/environment-data')
  }
}
</script>

<style scoped>
.device-detail-panel {
  padding: 0;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}
</style>
