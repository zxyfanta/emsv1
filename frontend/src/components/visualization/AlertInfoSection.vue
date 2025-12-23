<template>
  <div class="alert-info-section">
    <div class="section-header">
      <h3 class="section-title">
        <el-icon><Bell /></el-icon>
        告警信息
      </h3>
      <!-- 预留：告警筛选按钮 -->
      <el-button size="small" text>
        <el-icon><Filter /></el-icon>
      </el-button>
    </div>

    <!-- 告警列表区域（预留滚动容器） -->
    <el-scrollbar class="alert-list-container">
      <div class="empty-state">
        <el-icon :size="64" color="#52c41a">
          <CircleCheck />
        </el-icon>
        <p class="empty-text">暂无告警</p>
        <p class="empty-hint">系统运行正常</p>
      </div>
      <!-- 预留：告警列表项
      <div class="alert-list">
        <div v-for="alert in alerts" :key="alert.id" class="alert-item">
          ...
        </div>
      </div>
      -->
    </el-scrollbar>
  </div>
</template>

<script setup>
import { Bell, Filter, CircleCheck } from '@element-plus/icons-vue'

/**
 * 告警信息栏组件
 *
 * 当前状态：预留位置，显示"暂无告警"占位
 *
 * 后续扩展：
 * 1. 接入告警数据API
 * 2. 实现告警列表展示
 * 3. 添加告警筛选功能（按级别、设备、时间等）
 * 4. 实时推送告警信息（WebSocket）
 * 5. 告警声音提醒
 */
</script>

<style scoped>
.alert-info-section {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.alert-list-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.alert-list-container :deep(.el-scrollbar__wrap) {
  overflow-x: hidden;
}

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

/* 预留：告警列表项样式
.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  padding: 12px;
  background: rgba(255, 77, 79, 0.1);
  border: 1px solid rgba(255, 77, 79, 0.3);
  border-radius: 6px;
}

.alert-item.warning {
  background: rgba(250, 140, 22, 0.1);
  border-color: rgba(250, 140, 22, 0.3);
}

.alert-item.info {
  background: rgba(24, 144, 255, 0.1);
  border-color: rgba(24, 144, 255, 0.3);
}
*/
</style>
