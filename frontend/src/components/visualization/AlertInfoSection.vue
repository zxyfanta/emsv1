<template>
  <div class="alert-info-section">
    <div class="section-header">
      <h3 class="section-title">
        <el-icon><Bell /></el-icon>
        告警信息
      </h3>
      <el-badge :value="alerts.length" :max="99" class="alert-badge">
        <el-button size="small" text>
          <el-icon><Filter /></el-icon>
        </el-button>
      </el-badge>
    </div>

    <!-- 告警列表区域 -->
    <el-scrollbar class="alert-list-container">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="3" animated />
      </div>

      <!-- 错误状态 -->
      <div v-else-if="error" class="error-state">
        <el-icon :size="48" color="#ff4d4f"><CircleClose /></el-icon>
        <p class="error-text">加载失败</p>
        <el-button size="small" type="primary" @click="fetchAlerts">重试</el-button>
      </div>

      <!-- 空状态 -->
      <div v-else-if="alerts.length === 0" class="empty-state">
        <el-icon :size="64" color="#52c41a">
          <CircleCheck />
        </el-icon>
        <p class="empty-text">暂无告警</p>
        <p class="empty-hint">系统运行正常</p>
      </div>

      <!-- 告警列表 -->
      <div v-else class="alert-list">
        <div
          v-for="alert in alerts"
          :key="alert.id"
          class="alert-item"
          :class="[getSeverityClass(alert.severity), { 'new-alert': alert.isNew }]"
        >
          <div class="alert-header">
            <el-tag
              :type="getSeverityTagType(alert.severity)"
              size="small"
              class="alert-type-tag"
            >
              {{ alert.alertTypeDescription }}
            </el-tag>
            <span class="alert-time">{{ formatRelativeTime(alert.createdAt) }}</span>
          </div>

          <div class="alert-content">
            <div class="alert-device">
              <el-icon><Monitor /></el-icon>
              {{ alert.deviceCode }}
            </div>
            <div class="alert-message">{{ alert.message }}</div>
          </div>
        </div>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Bell, Filter, CircleCheck, CircleClose, Monitor } from '@element-plus/icons-vue'
import { getRecentAlerts } from '@/api/alert'
import { subscribeAlert } from '@/utils/sse'

const MAX_ALERTS = 10
const INITIAL_LOAD = 5

const alerts = ref([])
const loading = ref(false)
const error = ref(null)
let unsubscribe = null

/**
 * 获取初始告警数据
 */
const fetchAlerts = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await getRecentAlerts(INITIAL_LOAD)
    console.log('[AlertInfoSection] API响应:', res)

    // 尝试多种可能的响应路径
    let alertList = null
    if (res.data && res.data.data) {
      alertList = res.data.data
    } else if (res.data) {
      alertList = res.data
    } else if (Array.isArray(res)) {
      alertList = res
    }

    if (alertList && Array.isArray(alertList)) {
      alerts.value = alertList.map(alert => ({ ...alert, isNew: false }))
      console.log(`[AlertInfoSection] 成功加载 ${alerts.value.length} 条告警`)
    } else {
      console.warn('[AlertInfoSection] 响应数据格式不正确:', res)
    }
  } catch (err) {
    error.value = err
    console.error('[AlertInfoSection] 获取告警失败:', err)
  } finally {
    loading.value = false
  }
}

/**
 * 处理新告警（从SSE实时推送）
 */
const handleNewAlert = (sseData) => {
  console.log('[AlertInfoSection] 收到新告警推送:', sseData)

  // SSE推送的数据结构：{ alertId, alertType, severity, deviceCode, message, timestamp }
  const newAlert = {
    id: sseData.alertId,
    alertType: sseData.alertType,
    alertTypeDescription: getAlertTypeDescription(sseData.alertType),
    severity: sseData.severity,
    severityDescription: getSeverityDescription(sseData.severity),
    deviceCode: sseData.deviceCode,
    message: sseData.message,
    createdAt: sseData.timestamp,
    isNew: true  // 标记为新告警，用于闪烁动画
  }

  // 去重检查
  if (alerts.value.some(a => a.id === newAlert.id)) {
    console.log('[AlertInfoSection] 告警已存在，跳过:', newAlert.id)
    return
  }

  // 插入到头部
  alerts.value.unshift(newAlert)
  console.log(`[AlertInfoSection] 新告警已添加，当前告警数: ${alerts.value.length}`)

  // 移除isNew标记（3秒后，动画结束）
  setTimeout(() => {
    const alert = alerts.value.find(a => a.id === newAlert.id)
    if (alert) {
      alert.isNew = false
    }
  }, 3000)

  // 保持最多MAX_ALERTS条
  if (alerts.value.length > MAX_ALERTS) {
    const removed = alerts.value.pop()
    console.log('[AlertInfoSection] 告警列表已满，移除最旧告警:', removed.id)
  }
}

/**
 * 获取严重程度样式类名
 */
const getSeverityClass = (severity) => {
  const map = {
    'CRITICAL': 'critical',
    'WARNING': 'warning',
    'INFO': 'info'
  }
  return map[severity] || 'info'
}

/**
 * 获取严重程度标签类型（Element Plus Tag）
 */
const getSeverityTagType = (severity) => {
  const map = {
    'CRITICAL': 'danger',
    'WARNING': 'warning',
    'INFO': 'primary'
  }
  return map[severity] || 'info'
}

/**
 * 获取告警类型描述（备用）
 */
const getAlertTypeDescription = (alertType) => {
  const map = {
    'HIGH_CPM': '高CPM',
    'OFFLINE': '设备离线',
    'FAULT': '设备故障',
    'LOW_BATTERY': '低电量',
    'CPM_RISE': '辐射突增'
  }
  return map[alertType] || alertType
}

/**
 * 获取严重程度描述（备用）
 */
const getSeverityDescription = (severity) => {
  const map = {
    'CRITICAL': '高危',
    'WARNING': '警告',
    'INFO': '提示'
  }
  return map[severity] || severity
}

/**
 * 格式化相对时间
 */
const formatRelativeTime = (timestamp) => {
  if (!timestamp) return ''

  const now = new Date()
  const time = new Date(timestamp)
  const diff = Math.floor((now - time) / 1000) // 秒

  if (diff < 60) {
    return '刚刚'
  } else if (diff < 3600) {
    return `${Math.floor(diff / 60)}分钟前`
  } else if (diff < 86400) {
    return `${Math.floor(diff / 3600)}小时前`
  } else {
    return time.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  }
}

/**
 * 组件挂载
 */
onMounted(() => {
  console.log('[AlertInfoSection] 组件挂载')
  fetchAlerts()

  // 订阅SSE告警事件
  unsubscribe = subscribeAlert(handleNewAlert)
  console.log('[AlertInfoSection] 已订阅SSE告警事件')
})

/**
 * 组件卸载
 */
onBeforeUnmount(() => {
  if (unsubscribe) {
    unsubscribe()
    console.log('[AlertInfoSection] 已取消订阅SSE告警事件')
  }
})
</script>

<style scoped>
.alert-info-section {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: rgba(10, 25, 47, 0.5);
  border-radius: 8px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(24, 144, 255, 0.1);
  border-bottom: 1px solid rgba(24, 144, 255, 0.3);
  border-radius: 8px 8px 0 0;
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

.alert-badge {
  flex-shrink: 0;
}

.alert-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.alert-list-container :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

/* 加载状态 */
.loading-state {
  padding: 20px;
}

/* 错误状态 */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 200px;
  color: rgba(255, 255, 255, 0.5);
}

.error-text {
  margin: 12px 0;
  font-size: 14px;
  color: #ff4d4f;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 200px;
  color: rgba(255, 255, 255, 0.5);
}

.empty-text {
  margin: 16px 0 8px;
  font-size: 16px;
  font-weight: 500;
  color: #52c41a;
}

.empty-hint {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.4);
}

/* 告警列表 */
.alert-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 告警卡片 */
.alert-item {
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  transition: all 0.3s ease;
  cursor: pointer;
}

.alert-item:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: translateX(4px);
}

/* 严重程度颜色 */
.alert-item.critical {
  border-color: rgba(255, 77, 79, 0.5);
  background: linear-gradient(135deg, rgba(255, 77, 79, 0.15) 0%, rgba(255, 77, 79, 0.05) 100%);
}

.alert-item.warning {
  border-color: rgba(250, 173, 20, 0.5);
  background: linear-gradient(135deg, rgba(250, 173, 20, 0.15) 0%, rgba(250, 173, 20, 0.05) 100%);
}

.alert-item.info {
  border-color: rgba(24, 144, 255, 0.5);
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.15) 0%, rgba(24, 144, 255, 0.05) 100%);
}

/* 新告警闪烁动画 */
.alert-item.new-alert {
  animation: alertFlash 0.6s ease-in-out 3;
  border-color: #faad14;
  box-shadow: 0 0 20px rgba(250, 173, 20, 0.5);
}

@keyframes alertFlash {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(1.02);
  }
}

/* 告警头部 */
.alert-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.alert-type-tag {
  font-weight: 500;
}

.alert-time {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

/* 告警内容 */
.alert-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.alert-device {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.85);
}

.alert-message {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 滚动条样式 */
.alert-list-container :deep(.el-scrollbar__thumb) {
  background: rgba(24, 144, 255, 0.3);
  border-radius: 4px;
}

.alert-list-container :deep(.el-scrollbar__thumb:hover) {
  background: rgba(24, 144, 255, 0.5);
}
</style>
