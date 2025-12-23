<template>
  <div class="video-card" @click="$emit('click', device)">
    <!-- 16:9 缩略图区域 -->
    <div class="thumbnail-area">
      <img
        :src="thumbnailUrl"
        :alt="device.deviceName"
        class="thumbnail-image"
      />
      <!-- 在线状态标识 -->
      <div :class="['status-badge', device.status === 'ONLINE' ? 'online' : 'offline']">
        {{ device.status === 'ONLINE' ? '在线' : '离线' }}
      </div>
      <!-- 设备类型标识 -->
      <div class="device-type-badge">
        <el-tag
          :type="device.deviceType === 'RADIATION_MONITOR' ? 'danger' : 'success'"
          size="small"
        >
          {{ device.deviceType === 'RADIATION_MONITOR' ? '辐射' : '环境' }}
        </el-tag>
      </div>
      <!-- 播放图标 -->
      <div class="play-icon">
        <el-icon :size="32">
          <VideoPlay />
        </el-icon>
      </div>
    </div>

    <!-- 设备信息 -->
    <div class="device-info">
      <div class="device-name">{{ device.deviceName }}</div>
      <div class="device-code">{{ device.deviceCode }}</div>
      <div v-if="device.location" class="device-location">
        <el-icon><Location /></el-icon>
        {{ device.location }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { VideoPlay, Location } from '@element-plus/icons-vue'
import { getMockSnapshotUrl } from '@/api/video'

const props = defineProps({
  device: {
    type: Object,
    required: true
  }
})

defineEmits(['click'])

// 使用模拟缩略图（后续可替换为真实API）
const thumbnailUrl = computed(() => getMockSnapshotUrl(props.device.id))
</script>

<style scoped>
.video-card {
  background: rgba(24, 144, 255, 0.05);
  border: 1px solid rgba(24, 144, 255, 0.2);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.video-card:hover {
  background: rgba(24, 144, 255, 0.1);
  border-color: rgba(24, 144, 255, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.3);
}

.thumbnail-area {
  position: relative;
  width: 100%;
  padding-top: 56.25%; /* 16:9 比例 */
  background: #0a1929;
  overflow: hidden;
}

.thumbnail-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.8;
  transition: opacity 0.3s;
}

.video-card:hover .thumbnail-image {
  opacity: 1;
}

.status-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.online {
  background: rgba(82, 196, 26, 0.9);
  color: white;
}

.status-badge.offline {
  background: rgba(255, 77, 79, 0.9);
  color: white;
}

.device-type-badge {
  position: absolute;
  top: 8px;
  right: 8px;
}

.play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: rgba(255, 255, 255, 0.8);
  opacity: 0;
  transition: all 0.3s;
}

.video-card:hover .play-icon {
  opacity: 1;
  transform: translate(-50%, -50%) scale(1.1);
}

.device-info {
  padding: 10px 12px;
}

.device-name {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-code {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 6px;
}

.device-location {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}
</style>
