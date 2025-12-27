<template>
  <div class="alert-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>告警管理</span>
          <el-button type="primary" @click="loadData" :icon="Refresh">刷新</el-button>
        </div>
      </template>

      <!-- 统计卡片 -->
      <el-row :gutter="20" style="margin-bottom: 20px">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon" style="background: #F56C6C">
                <el-icon><Bell /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statistics.totalUnresolved }}</div>
                <div class="stat-label">未解决告警</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon" style="background: #E6A23C">
                <el-icon><Warning /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statistics.bySeverity.HIGH || 0 }}</div>
                <div class="stat-label">高危告警</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon" style="background: #409EFF">
                <el-icon><InfoFilled /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statistics.bySeverity.MEDIUM || 0 }}</div>
                <div class="stat-label">中等告警</div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <div class="stat-icon" style="background: #67C23A">
                <el-icon><SuccessFilled /></el-icon>
              </div>
              <div class="stat-info">
                <div class="stat-value">{{ statistics.bySeverity.LOW || 0 }}</div>
                <div class="stat-label">低危告警</div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 筛选表单 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="告警状态">
          <el-select v-model="searchForm.resolved" placeholder="请选择" clearable>
            <el-option label="未解决" :value="false" />
            <el-option label="已解决" :value="true" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 告警表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="deviceCode" label="设备编码" width="120" />
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="alertTypeDescription" label="告警类型" width="120" />
        <el-table-column prop="severityDescription" label="严重程度" width="100">
          <template #default="{ row }">
            <el-tag :type="getSeverityTagType(row.severity)">
              {{ row.severityDescription }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="告警消息" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="发生时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="resolved" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.resolved ? 'success' : 'danger'">
              {{ row.resolved ? '已解决' : '未解决' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.resolved"
              link
              type="primary"
              @click="handleResolve(row)"
            >
              标记解决
            </el-button>
            <el-button link type="primary" @click="handleViewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 告警详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="告警详情"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-descriptions :column="1" border v-if="selectedAlert">
        <el-descriptions-item label="设备编码">
          {{ selectedAlert.deviceCode }}
        </el-descriptions-item>
        <el-descriptions-item label="设备名称">
          {{ selectedAlert.deviceName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="告警类型">
          <el-tag :type="getSeverityTagType(selectedAlert.severity)">
            {{ selectedAlert.alertTypeDescription }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="严重程度">
          {{ selectedAlert.severityDescription }}
        </el-descriptions-item>
        <el-descriptions-item label="告警消息">
          {{ selectedAlert.message }}
        </el-descriptions-item>
        <el-descriptions-item label="发生时间">
          {{ formatDate(selectedAlert.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedAlert.resolved ? 'success' : 'danger'">
            {{ selectedAlert.resolved ? '已解决' : '未解决' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="解决时间" v-if="selectedAlert.resolved">
          {{ formatDate(selectedAlert.resolvedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="详细数据" v-if="selectedAlert.data">
          <pre class="alert-data">{{ JSON.stringify(selectedAlert.data, null, 2) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAlertList, resolveAlert, getAlertStatistics } from '@/api/alert'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  Warning,
  InfoFilled,
  SuccessFilled,
  Refresh
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const loading = ref(false)
const tableData = ref([])
const detailDialogVisible = ref(false)
const selectedAlert = ref(null)

const searchForm = reactive({
  resolved: null
})

const pagination = reactive({
  page: 0,
  size: 20,
  total: 0
})

const statistics = ref({
  totalUnresolved: 0,
  bySeverity: {
    HIGH: 0,
    MEDIUM: 0,
    LOW: 0
  }
})

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res = await getAlertStatistics()
    if (res.status === 200) {
      statistics.value = res.data
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载告警列表
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      ...searchForm
    }
    const res = await getAlertList(params)
    if (res.status === 200) {
      tableData.value = res.data.content
      pagination.total = res.data.totalElements
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 0
  loadData()
}

const handleReset = () => {
  searchForm.resolved = null
  handleSearch()
}

const handleResolve = (row) => {
  ElMessageBox.confirm(`确定要标记告警为已解决吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await resolveAlert(row.id)
      ElMessage.success('告警已解决')
      loadData()
      loadStatistics()
    } catch (error) {
      ElMessage.error('操作失败')
    }
  })
}

const handleViewDetail = (row) => {
  selectedAlert.value = row
  detailDialogVisible.value = true
}

const getSeverityTagType = (severity) => {
  const typeMap = {
    'HIGH': 'danger',
    'MEDIUM': 'warning',
    'LOW': 'success'
  }
  return typeMap[severity] || 'info'
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

onMounted(() => {
  loadStatistics()
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-card {
  height: 100px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 28px;
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

.search-form {
  margin-bottom: 20px;
}

.alert-data {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 200px;
  overflow-y: auto;
}
</style>
