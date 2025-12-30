<template>
  <div class="backup-management">
    <el-card class="header-card">
      <template #header>
        <div class="card-header">
          <h2>数据备份管理</h2>
          <el-tag type="info">每月自动备份</el-tag>
        </div>
      </template>

      <!-- 备份状态卡片 -->
      <el-row :gutter="20" class="status-cards">
        <el-col :span="8">
          <el-card shadow="hover" class="status-card">
            <div class="card-content">
              <div class="icon">
                <el-icon size="40"><DataLine /></el-icon>
              </div>
              <div class="info">
                <h3>时序数据</h3>
                <p>保留: {{ timeseriesRetention }}个月</p>
                <el-tag :type="timeseriesStatus ? 'success' : 'info'" size="small">
                  {{ timeseriesStatus ? '已启用' : '未启用' }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover" class="status-card">
            <div class="card-content">
              <div class="icon">
                <el-icon size="40"><Document /></el-icon>
              </div>
              <div class="info">
                <h3>业务数据</h3>
                <p>保留: {{ businessRetention }}个月</p>
                <el-tag :type="businessStatus ? 'success' : 'info'" size="small">
                  {{ businessStatus ? '已启用' : '未启用' }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover" class="status-card">
            <div class="card-content">
              <div class="icon">
                <el-icon size="40"><Setting /></el-icon>
              </div>
              <div class="info">
                <h3>系统配置</h3>
                <p>保留: {{ systemKeepCount }}次备份</p>
                <el-tag :type="systemStatus ? 'success' : 'info'" size="small">
                  {{ systemStatus ? '已启用' : '未启用' }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 立即执行备份 -->
      <div class="action-buttons">
        <h3>备份操作</h3>
        <div class="buttons">
          <el-button
            type="primary"
            :icon="VideoPlay"
            :loading="backupLoading.all"
            @click="executeFullBackup"
          >
            立即执行全量备份
          </el-button>
          <el-button
            :icon="VideoPlay"
            :loading="backupLoading.timeseries"
            @click="executeTimeseriesBackup"
          >
            备份时序数据
          </el-button>
          <el-button
            :icon="VideoPlay"
            :loading="backupLoading.business"
            @click="executeBusinessBackup"
          >
            备份业务数据
          </el-button>
          <el-button
            :icon="VideoPlay"
            :loading="backupLoading.system"
            @click="executeSystemBackup"
          >
            备份系统配置
          </el-button>
        </div>
      </div>

      <!-- 定时任务配置 -->
      <div class="scheduler-config">
        <h3>定时任务配置</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="执行频率">
            每月1次（每月1号凌晨3点）
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag type="success">启用</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 备份统计 -->
      <div class="backup-statistics">
        <h3>备份统计</h3>
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="stat-item">
              <div class="value">{{ statistics.total || 0 }}</div>
              <div class="label">总备份次数</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item success">
              <div class="value">{{ statistics.success || 0 }}</div>
              <div class="label">成功</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item danger">
              <div class="value">{{ statistics.failed || 0 }}</div>
              <div class="label">失败</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-item running">
              <div class="value">{{ statistics.running || 0 }}</div>
              <div class="label">运行中</div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 备份历史 -->
    <el-card class="history-card">
      <template #header>
        <h3>备份历史</h3>
      </template>

      <el-table
        :data="backupLogs"
        v-loading="loading"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="createdAt" label="备份时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="backupType" label="备份类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getBackupTypeTag(row.backupType)">
              {{ getBackupTypeName(row.backupType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="triggerMode" label="触发方式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.triggerMode === 'MANUAL' ? 'warning' : 'info'" size="small">
              {{ row.triggerMode === 'MANUAL' ? '手动' : '定时' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="recordCount" label="记录数" width="100">
          <template #default="{ row }">
            {{ row.recordCount?.toLocaleString() || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="执行耗时" width="120">
          <template #default="{ row }">
            {{ formatDuration(row.durationMs) }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="200">
          <template #default="{ row }">
            <span v-if="row.status === 'FAILED'" class="error-message">
              {{ row.errorMessage }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, DataLine, Document, Setting } from '@element-plus/icons-vue'
import backupApi from '@/api/backup'

// 配置信息
const timeseriesRetention = ref(6)
const businessRetention = ref(6)
const systemKeepCount = ref(2)
const timeseriesStatus = ref(true)
const businessStatus = ref(true)
const systemStatus = ref(true)

// 备份统计
const statistics = reactive({
  total: 0,
  success: 0,
  failed: 0,
  running: 0
})

// 备份日志
const backupLogs = ref([])
const loading = ref(false)

// 备份加载状态
const backupLoading = reactive({
  all: false,
  timeseries: false,
  business: false,
  system: false
})

// 加载备份统计
const loadStatistics = async () => {
  try {
    const response = await backupApi.getStatistics()
    Object.assign(statistics, response.data)
  } catch (error) {
    console.error('加载备份统计失败:', error)
  }
}

// 加载备份历史
const loadBackupLogs = async () => {
  loading.value = true
  try {
    const response = await backupApi.getRecentBackups()
    backupLogs.value = response.data || []
  } catch (error) {
    console.error('加载备份历史失败:', error)
    ElMessage.error('加载备份历史失败')
  } finally {
    loading.value = false
  }
}

// 执行全量备份
const executeFullBackup = async () => {
  backupLoading.all = true
  try {
    await backupApi.executeFullBackup()
    ElMessage.success('全量备份任务已启动')
    setTimeout(() => {
      loadBackupLogs()
      loadStatistics()
    }, 1000)
  } catch (error) {
    ElMessage.error('启动全量备份失败')
  } finally {
    backupLoading.all = false
  }
}

// 执行时序数据备份
const executeTimeseriesBackup = async () => {
  backupLoading.timeseries = true
  try {
    await backupApi.executeTimeseriesBackup()
    ElMessage.success('时序数据备份任务已启动')
    setTimeout(() => {
      loadBackupLogs()
      loadStatistics()
    }, 1000)
  } catch (error) {
    ElMessage.error('启动时序数据备份失败')
  } finally {
    backupLoading.timeseries = false
  }
}

// 执行业务数据备份
const executeBusinessBackup = async () => {
  backupLoading.business = true
  try {
    await backupApi.executeBusinessBackup()
    ElMessage.success('业务数据备份任务已启动')
    setTimeout(() => {
      loadBackupLogs()
      loadStatistics()
    }, 1000)
  } catch (error) {
    ElMessage.error('启动业务数据备份失败')
  } finally {
    backupLoading.business = false
  }
}

// 执行系统配置备份
const executeSystemBackup = async () => {
  backupLoading.system = true
  try {
    await backupApi.executeSystemBackup()
    ElMessage.success('系统配置备份任务已启动')
    setTimeout(() => {
      loadBackupLogs()
      loadStatistics()
    }, 1000)
  } catch (error) {
    ElMessage.error('启动系统配置备份失败')
  } finally {
    backupLoading.system = false
  }
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

// 获取备份类型标签类型
const getBackupTypeTag = (type) => {
  const tags = {
    TIMESERIES: 'primary',
    BUSINESS: 'success',
    SYSTEM: 'warning',
    FULL: 'danger'
  }
  return tags[type] || 'info'
}

// 获取备份类型名称
const getBackupTypeName = (type) => {
  const names = {
    TIMESERIES: '时序数据',
    BUSINESS: '业务数据',
    SYSTEM: '系统配置',
    FULL: '全量备份'
  }
  return names[type] || type
}

// 获取状态标签类型
const getStatusTag = (status) => {
  const tags = {
    RUNNING: 'warning',
    SUCCESS: 'success',
    FAILED: 'danger',
    PARTIAL: 'info'
  }
  return tags[status] || 'info'
}

// 获取状态名称
const getStatusName = (status) => {
  const names = {
    RUNNING: '运行中',
    SUCCESS: '成功',
    FAILED: '失败',
    PARTIAL: '部分成功'
  }
  return names[status] || status
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(2) + ' MB'
  return (bytes / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

// 格式化执行耗时
const formatDuration = (ms) => {
  if (!ms) return '-'
  if (ms < 1000) return ms + ' ms'
  if (ms < 60000) return (ms / 1000).toFixed(2) + ' 秒'
  return (ms / 60000).toFixed(2) + ' 分钟'
}

// 组件挂载时加载数据
onMounted(() => {
  loadStatistics()
  loadBackupLogs()
})
</script>

<style scoped>
.backup-management {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.status-cards {
  margin-bottom: 30px;
}

.status-card {
  height: 120px;
}

.card-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
}

.info p {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #909399;
}

.action-buttons {
  margin-bottom: 30px;
}

.action-buttons h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: 600;
}

.buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.scheduler-config {
  margin-bottom: 30px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.scheduler-config h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: 600;
}

.backup-statistics {
  margin-bottom: 30px;
}

.backup-statistics h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  font-weight: 600;
}

.stat-item {
  text-align: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.stat-item .value {
  font-size: 28px;
  font-weight: 700;
  color: #409eff;
  margin-bottom: 8px;
}

.stat-item.success .value {
  color: #67c23a;
}

.stat-item.danger .value {
  color: #f56c6c;
}

.stat-item.running .value {
  color: #e6a23c;
}

.stat-item .label {
  font-size: 14px;
  color: #909399;
}

.history-card {
  margin-top: 20px;
}

.history-card h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.error-message {
  color: #f56c6c;
}
</style>
