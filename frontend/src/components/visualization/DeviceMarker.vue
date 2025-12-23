<template>
  <TresGroup :position="position" @click="handleClick">
    <!-- 设备标记球体 -->
    <TresMesh>
      <TresSphereGeometry :args="[0.5, 32, 32]" />
      <TresMeshStandardMaterial :color="markerColor" />
    </TresMesh>

    <!-- 设备标签 (使用HTML) -->
    <Html
      :position="[0, 1, 0]"
      :center="true"
      :distance-factor="15"
    >
      <div class="device-label" :class="labelClass">
        <div class="device-type">{{ deviceTypeLabel }}</div>
        <div class="device-name">{{ device.deviceName }}</div>
        <div class="device-code">{{ device.deviceCode }}</div>
      </div>
    </Html>
  </TresGroup>
</template>

<script setup>
import { computed } from 'vue'
import { Html } from '@tresjs/cientos'

const props = defineProps({
  device: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['click'])

// 将0-100坐标映射到3D空间坐标
// X轴：-20到+20 (对应positionX 0-100)
// Z轴：-20到+20 (对应positionY 0-100，在3D中Z是平面的另一个轴)
const position = computed(() => {
  const x = ((props.device.positionX || 50) - 50) * 0.4  // 映射到-20~20
  const z = ((props.device.positionY || 50) - 50) * 0.4  // 映射到-20~20
  return [x, 0.5, z]
})

// 根据设备类型获取标签
const deviceTypeLabel = computed(() => {
  return props.device.deviceType === 'RADIATION_MONITOR' ? '辐射设备' : '环境设备'
})

// 根据设备类型和状态选择颜色
const markerColor = computed(() => {
  const isOnline = props.device.status === 'ONLINE'
  const isRadiation = props.device.deviceType === 'RADIATION_MONITOR'

  if (!isOnline) {
    return isRadiation ? '#8b0000' : '#2e4d28'  // 离线：深红色/深绿色
  }
  return isRadiation ? '#ff4444' : '#44cc44'  // 在线：亮红色/亮绿色
})

// 标签样式类
const labelClass = computed(() => {
  return {
    'radiation': props.device.deviceType === 'RADIATION_MONITOR',
    'environment': props.device.deviceType === 'ENVIRONMENT_STATION',
    'offline': props.device.status !== 'ONLINE'
  }
})

// 处理点击
const handleClick = () => {
  emit('click', props.device)
}
</script>

<style scoped>
.device-label {
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
  white-space: nowrap;
  pointer-events: none;
  transition: all 0.3s;
}

.device-label.radiation {
  border-left: 3px solid #ff4444;
}

.device-label.environment {
  border-left: 3px solid #44cc44;
}

.device-label.offline {
  opacity: 0.6;
}

.device-type {
  font-weight: bold;
  margin-bottom: 4px;
}

.device-name {
  font-size: 14px;
  margin-bottom: 2px;
}

.device-code {
  font-size: 11px;
  opacity: 0.8;
}
</style>
