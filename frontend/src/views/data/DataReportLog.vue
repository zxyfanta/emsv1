<template>
  <div class="data-report-log">
    <el-card>
      <template #header>
        <span>数据上报日志</span>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="设备">
          <el-select v-model="searchForm.deviceId" placeholder="请选择设备" clearable filterable>
            <el-option
              v-for="device in devices"
              :key="device.id"
              :label="`${device.deviceName} (${device.deviceCode})`"
              :value="device.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="上报协议">
          <el-select v-model="searchForm.reportProtocol" placeholder="请选择" clearable>
            <el-option label="四川协议" value="SICHUAN" />
            <el-option label="山东协议" value="SHANDONG" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="device.deviceCode" label="设备编码" width="150">
          <template #default="{ row }">
            {{ row.device?.deviceCode || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="device.deviceName" label="设备名称" width="150">
          <template #default="{ row }">
            {{ row.device?.deviceName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="reportProtocol" label="上报协议" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.reportProtocol" :type="row.reportProtocol === 'SICHUAN' ? 'primary' : 'success'" size="small">
              {{ row.reportProtocol === 'SICHUAN' ? '四川协议' : '山东协议' }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reportTime" label="上报时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.reportTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column prop="errorMessage" label="错误信息" min-width="200">
          <template #default="{ row }">
            <span v-if="row.status === 'FAILED'" class="error-message">
              {{ row.errorMessage || '-' }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 20px"
      />
    </el-card>

    <!-- 日志详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="上报日志详情"
      width="700px"
    >
      <el-descriptions :column="2" border v-if="selectedLog">
        <el-descriptions-item label="设备编码">
          {{ selectedLog.device?.deviceCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="设备名称">
          {{ selectedLog.device?.deviceName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="上报协议">
          <el-tag v-if="selectedLog.reportProtocol" :type="selectedLog.reportProtocol === 'SICHUAN' ? 'primary' : 'success'" size="small">
            {{ selectedLog.reportProtocol === 'SICHUAN' ? '四川协议' : '山东协议' }}
          </el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="selectedLog.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
            {{ selectedLog.status === 'SUCCESS' ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="上报时间" :span="2">
          {{ formatDate(selectedLog.reportTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="HTTP状态码" v-if="selectedLog.httpStatus">
          {{ selectedLog.httpStatus }}
        </el-descriptions-item>
        <el-descriptions-item label="耗时">
          {{ selectedLog.durationMs }} ms
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="selectedLog.status === 'FAILED'">
          <div class="error-message-detail">
            {{ selectedLog.errorMessage || '-' }}
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="请求内容" :span="2" v-if="selectedLog.requestPayload">
          <pre class="report-data">{{ formatJson(selectedLog.requestPayload) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="响应内容" :span="2" v-if="selectedLog.responseBody">
          <pre class="report-data">{{ formatJson(selectedLog.responseBody) }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getDeviceReportLogs } from '@/api/data'
import { getDeviceList } from '@/api/device'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const loading = ref(false)
const tableData = ref([])
const devices = ref([])
const detailDialogVisible = ref(false)
const selectedLog = ref(null)

const searchForm = reactive({
  deviceId: null,
  reportProtocol: '',
  status: ''
})

const pagination = reactive({
  page: 0,
  size: 20,
  total: 0
})

const loadDevices = async () => {
  try {
    const res = await getDeviceList({ page: 0, size: 1000 })
    if (res.status === 200) {
      devices.value = res.data.content
    }
  } catch (error) {
    console.error('加载设备列表失败:', error)
  }
}

const loadData = async () => {
  if (!searchForm.deviceId) {
    ElMessage.warning('请先选择设备')
    return
  }

  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }

    // 添加筛选条件
    if (searchForm.reportProtocol) {
      params.reportProtocol = searchForm.reportProtocol
    }
    if (searchForm.status) {
      params.status = searchForm.status
    }

    const res = await getDeviceReportLogs(searchForm.deviceId, params)
    if (res.status === 200) {
      tableData.value = res.data.content
      pagination.total = res.data.totalElements
    }
  } catch (error) {
    ElMessage.error('加载日志数据失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 0
  loadData()
}

const handleReset = () => {
  searchForm.deviceId = null
  searchForm.reportProtocol = ''
  searchForm.status = ''
  pagination.page = 0
  tableData.value = []
  pagination.total = 0
}

const handleViewDetail = (row) => {
  selectedLog.value = row
  detailDialogVisible.value = true
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

const formatJson = (data) => {
  try {
    if (typeof data === 'string') {
      return JSON.stringify(JSON.parse(data), null, 2)
    }
    return JSON.stringify(data, null, 2)
  } catch {
    return data || '-'
  }
}

onMounted(async () => {
  await loadDevices()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}

.error-message {
  color: #f56c6c;
  font-size: 12px;
}

.error-message-detail {
  color: #f56c6c;
  font-size: 14px;
  word-break: break-all;
  white-space: pre-wrap;
}

.report-data {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 300px;
  overflow-y: auto;
  margin: 0;
}
</style>
