<template>
  <div class="video-card" @click="$emit('click', videoDevice)">
    <!-- 16:9 缩略图区域 -->
    <div class="thumbnail-area">
      <img
        :src="thumbnailUrl"
        :alt="videoDevice.deviceName"
        class="thumbnail-image"
      />

      <!-- 绑定状态标识 -->
      <div class="bind-status-badge">
        <el-tag
          :type="linkedDevice ? 'success' : 'info'"
          size="small"
        >
          {{ linkedDevice ? '已绑定' : '未绑定' }}
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
      <div class="video-device-name">{{ videoDevice.deviceName }}</div>
      <div class="video-device-code">{{ videoDevice.deviceCode }}</div>

      <!-- 绑定的监测设备信息 -->
      <div v-if="linkedDevice" class="linked-device-info">
        <el-icon><Link /></el-icon>
        <span>{{ linkedDevice.deviceName }}</span>
        <el-tag
          :type="linkedDevice.deviceType === 'RADIATION_MONITOR' ? 'danger' : 'success'"
          size="small"
          style="margin-left: 4px"
        >
          {{ linkedDevice.deviceType === 'RADIATION_MONITOR' ? '辐射' : '环境' }}
        </el-tag>
      </div>
      <div v-else class="no-bind-info">
        <el-icon><Warning /></el-icon>
        <span>未绑定监测设备</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { VideoPlay, Link, Warning } from '@element-plus/icons-vue'
import { getMockSnapshotUrl } from '@/api/video'

const props = defineProps({
  videoDevice: {
    type: Object,
    required: true
  },
  linkedDevice: {
    type: Object,
    default: null
  }
})

defineEmits(['click'])

// 使用模拟缩略图（后续可替换为真实API）
const thumbnailUrl = computed(() => {
  // 如果有snapshotUrl则使用，否则使用mock
  if (props.videoDevice.snapshotUrl) {
    return props.videoDevice.snapshotUrl
  }
  return getMockSnapshotUrl(props.videoDevice.id)
})
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

.bind-status-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
}

.play-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: rgba(255, 255, 255, 0.8);
  opacity: 0;
  transition: all 0.3s;
  z-index: 1;
}

.video-card:hover .play-icon {
  opacity: 1;
  transform: translate(-50%, -50%) scale(1.1);
}

.device-info {
  padding: 10px 12px;
}

.video-device-name {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.video-device-code {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 8px;
}

.linked-device-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  padding: 4px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.no-bind-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  padding: 4px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}
</style>
