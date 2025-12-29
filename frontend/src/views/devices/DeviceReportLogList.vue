<template>
  <div class="report-log-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>上报日志 - {{ deviceInfo?.deviceName }}</span>
          <el-button @click="handleBack">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="协议类型">
          <el-select v-model="searchForm.reportProtocol" placeholder="全部" clearable>
            <el-option label="四川协议" value="SICHUAN" />
            <el-option label="山东协议" value="SHANDONG" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志表格 -->
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="reportTime" label="上报时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.reportTime) }}
          </template>
        </el-table-column>

        <el-table-column prop="reportProtocol" label="协议" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.reportProtocol === 'SICHUAN' ? 'primary' : 'success'">
              {{ row.reportProtocol === 'SICHUAN' ? '四川' : '山东' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="durationMs" label="耗时" width="80">
          <template #default="{ row }">
            {{ row.durationMs }}ms
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button
              size="small"
              type="primary"
              link
              @click="handleViewError(row)"
              v-if="row.status === 'FAILED'"
            >
              查看错误
            </el-button>
            <span v-else class="text-gray">-</span>
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

    <!-- 错误详情对话框 -->
    <el-dialog
      v-model="errorDialogVisible"
      title="错误详情"
      width="600"
    >
      <el-descriptions :column="1" border v-if="currentLog">
        <el-descriptions-item label="上报时间">
          {{ formatTime(currentLog.reportTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="协议">
          {{ currentLog.reportProtocol === 'SICHUAN' ? '四川协议' : '山东协议' }}
        </el-descriptions-item>
        <el-descriptions-item label="耗时">
          {{ currentLog.durationMs }}ms
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" v-if="currentLog.errorMessage">
          <el-text type="danger" tag="pre">{{ currentLog.errorMessage }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="请求内容" v-if="currentLog.requestPayload">
          <el-text tag="pre" class="pre-wrap">{{ formatJson(currentLog.requestPayload) }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="响应内容" v-if="currentLog.responseBody">
          <el-text tag="pre" class="pre-wrap">{{ formatJson(currentLog.responseBody) }}</el-text>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDeviceDetail } from '@/api/device'
import { getDeviceReportLogs } from '@/api/device'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()

const deviceId = route.params.id
const deviceInfo = ref(null)

const loading = ref(false)
const tableData = ref([])
const errorDialogVisible = ref(false)
const currentLog = ref(null)

const searchForm = reactive({
  reportProtocol: '',
  status: ''
})

const pagination = reactive({
  page: 0,
  size: 20,
  total: 0
})

// 加载设备信息
const loadDeviceInfo = async () => {
  try {
    const res = await getDeviceDetail(deviceId)
    if (res.status === 200) {
      deviceInfo.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载设备信息失败')
  }
}

// 加载日志数据
const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      ...searchForm
    }
    const res = await getDeviceReportLogs(deviceId, params)
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

// 查询
const handleSearch = () => {
  pagination.page = 0
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.reportProtocol = ''
  searchForm.status = ''
  handleSearch()
}

// 返回
const handleBack = () => {
  router.back()
}

// 查看错误
const handleViewError = (row) => {
  currentLog.value = row
  errorDialogVisible.value = true
}

// 格式化时间
const formatTime = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

// 格式化JSON
const formatJson = (jsonStr) => {
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch (error) {
    return jsonStr
  }
}

onMounted(() => {
  loadDeviceInfo()
  loadData()
})
</script>

<style scoped>
.report-log-list {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.text-gray {
  color: #909399;
}

.pre-wrap {
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 400px;
  overflow-y: auto;
}
</style>
