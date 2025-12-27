<template>
  <div class="right-panel-content">
    <!-- 1. 设备视频栏 - 直接传递设备列表，由VideoDeviceSection内部加载视频设备 -->
    <DeviceVideoSection :monitor-devices="devices" class="video-section" />

    <!-- 2. 实时信息栏 -->
    <RealtimeInfoSection :devices="onlineDevices" class="realtime-section" />

    <!-- 3. 告警信息栏 -->
    <AlertInfoSection class="alert-section" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import DeviceVideoSection from './DeviceVideoSection.vue'
import RealtimeInfoSection from './RealtimeInfoSection.vue'
import AlertInfoSection from './AlertInfoSection.vue'

const props = defineProps({
  devices: {
    type: Array,
    default: () => []
  }
})

// 获取在线设备用于实时信息展示
const onlineDevices = computed(() => {
  return props.devices.filter(d => d.status === 'ONLINE')
})
</script>

<style scoped>
.right-panel-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 50px 16px 16px 16px; /* 顶部留出panel-header空间 */
  overflow: hidden;
}

/* 设备视频栏 - 约35% */
.video-section {
  flex: 3.5;
  min-height: 0;
  overflow: hidden;
}

/* 实时信息栏 - 约40% */
.realtime-section {
  flex: 4.0;
  min-height: 0;
  overflow: hidden;
}

/* 告警信息栏 - 约25% */
.alert-section {
  flex: 2.5;
  min-height: 0;
  overflow: hidden;
}
</style>
