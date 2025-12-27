<template>
  <div class="alert-notification-container">
    <!-- 告警通知将通过 Element Plus 的 ElNotification 动态显示 -->
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount } from 'vue'
import { ElNotification } from 'element-plus'
import { Bell, Warning } from '@element-plus/icons-vue'
import { sseManager } from '@/utils/sse'
import router from '@/router'

let unsubscribeAlert = null

// 处理告警消息
const handleAlertMessage = (alertData) => {
  console.log('[告警通知] 收到告警:', alertData)

  // 根据严重程度决定通知类型
  let type = 'warning'
  let title = '告警通知'
  let duration = 0 // 手动关闭

  if (alertData.severity === 'HIGH') {
    type = 'error'
    title = '🚨 高危告警'
  } else if (alertData.severity === 'MEDIUM') {
    type = 'warning'
    title = '⚠️ 中等告警'
  } else {
    type = 'info'
    title = 'ℹ️ 低危告警'
    duration = 5000 // 5秒后自动关闭
  }

  // 显示通知
  ElNotification({
    title,
    message: alertData.message || alertData.alertTypeDescription || '检测到异常',
    type,
    duration,
    icon: alertData.severity === 'HIGH' ? Warning : Bell,
    onClick: () => {
      // 点击通知跳转到告警列表
      router.push('/alerts')
    },
    customClass: `alert-notification-${type}`
  })
}

onMounted(() => {
  // 订阅告警事件
  unsubscribeAlert = sseManager.subscribe('alert', handleAlertMessage)
})

onBeforeUnmount(() => {
  // 取消订阅
  if (unsubscribeAlert) {
    unsubscribeAlert()
  }
})
</script>

<style scoped>
.alert-notification-container {
  /* 这个组件主要用于逻辑处理，不需要实际渲染内容 */
}
</style>

<style>
/* 全局样式：告警通知样式增强 */
.alert-notification-error {
  border-left: 4px solid #F56C6C !important;
}

.alert-notification-warning {
  border-left: 4px solid #E6A23C !important;
}

.alert-notification-info {
  border-left: 4px solid #409EFF !important;
}

/* 告警通知可点击 */
.el-notification {
  cursor: pointer;
}

.el-notification:hover {
  opacity: 0.9;
}

/* 增强告警通知的视觉效果 */
.el-notification__group {
  cursor: pointer;
}

.el-notification__title {
  font-weight: bold;
}

.el-notification__content {
  margin-top: 8px;
  line-height: 1.5;
}
</style>
