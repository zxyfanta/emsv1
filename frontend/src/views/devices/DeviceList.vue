<template>
  <div class="device-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备列表</span>
          <div class="header-actions">
            <el-button type="success" @click="handleActivate">
              <el-icon><Plus /></el-icon>
              激活设备
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="设备编码">
          <el-input v-model="searchForm.deviceCode" placeholder="请输入设备编码" clearable />
        </el-form-item>
        <el-form-item label="设备类型">
          <el-select v-model="searchForm.deviceType" placeholder="请选择" clearable>
            <el-option label="辐射监测仪" value="RADIATION_MONITOR" />
            <el-option label="环境监测站" value="ENVIRONMENT_STATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="在线" value="ONLINE" />
            <el-option label="离线" value="OFFLINE" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="故障" value="FAULT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="deviceCode" label="设备编码" width="120" />
        <el-table-column prop="deviceName" label="设备名称" width="150" />
        <el-table-column prop="deviceType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.deviceType === 'RADIATION_MONITOR' ? 'danger' : 'success'">
              {{ row.deviceType === 'RADIATION_MONITOR' ? '辐射' : '环境' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 上报状态 -->
        <el-table-column label="上报状态" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.dataReportEnabled"
              @change="handleToggleReport(row)"
              :loading="row.updating"
            />
          </template>
        </el-table-column>

        <el-table-column prop="reportProtocol" label="协议" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.reportProtocol" size="small">
              {{ row.reportProtocol === 'SICHUAN' ? '四川' : '山东' }}
            </el-tag>
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="lastReportTime" label="最后上报" width="140">
          <template #default="{ row }">
            {{ formatTime(row.lastReportTime) || '未上报' }}
          </template>
        </el-table-column>

        <el-table-column label="成功率" width="100">
          <template #default="{ row }">
            <el-progress
              v-if="row.totalReportCount > 0"
              :percentage="calculateSuccessRate(row)"
              :color="getSuccessRateColor(calculateSuccessRate(row))"
              :stroke-width="8"
            />
            <span v-else class="text-gray">-</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleConfigReport(row)">
              配置上报
            </el-button>
            <el-button size="small" @click="handleViewLog(row)">
              日志
            </el-button>
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 设备详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="设备详情"
      width="600px"
      :close-on-click-modal="false"
    >
      <DeviceDetailPanel v-if="selectedDevice" :device="selectedDevice" />
    </el-dialog>

    <!-- 数据上报配置对话框 -->
    <DeviceReportConfigDialog
      v-model="reportConfigDialogVisible"
      :device="selectedDevice"
      @success="handleConfigSuccess"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getDeviceList, deleteDevice as deleteDeviceApi, updateDevice } from '@/api/device'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import DeviceDetailPanel from '@/components/visualization/DeviceDetailPanel.vue'
import DeviceReportConfigDialog from './DeviceReportConfigDialog.vue'

const router = useRouter()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.isAdmin)

const loading = ref(false)
const tableData = ref([])
const detailDialogVisible = ref(false)
const reportConfigDialogVisible = ref(false)
const selectedDevice = ref(null)

const searchForm = reactive({
  deviceCode: '',
  deviceType: '',
  status: ''
})

const pagination = reactive({
  page: 0,
  size: 10,
  total: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size,
      ...searchForm
    }
    const res = await getDeviceList(params)
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
  searchForm.deviceCode = ''
  searchForm.deviceType = ''
  searchForm.status = ''
  handleSearch()
}

const handleActivate = () => {
  router.push('/devices/activate')
}

const handleView = (row) => {
  selectedDevice.value = row
  detailDialogVisible.value = true
}

const handleEdit = (row) => {
  router.push(`/devices/${row.id}/edit`)
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除设备 "${row.deviceName}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteDeviceApi(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

const getStatusType = (status) => {
  const typeMap = {
    'ONLINE': 'success',
    'OFFLINE': 'info',
    'MAINTENANCE': 'warning',
    'FAULT': 'danger'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    'ONLINE': '在线',
    'OFFLINE': '离线',
    'MAINTENANCE': '维护中',
    'FAULT': '故障'
  }
  return textMap[status] || status
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

const formatTime = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : null
}

// 计算上报成功率
const calculateSuccessRate = (row) => {
  if (!row.totalReportCount || row.totalReportCount === 0) return 0
  return Math.round((row.successReportCount / row.totalReportCount) * 100)
}

// 获取成功率颜色
const getSuccessRateColor = (rate) => {
  if (rate >= 90) return '#67c23a'
  if (rate >= 70) return '#e6a23c'
  return '#f56c6c'
}

// 切换上报状态
const handleToggleReport = async (row) => {
  row.updating = true
  try {
    await updateDevice(row.id, {
      dataReportEnabled: row.dataReportEnabled
    })
    ElMessage.success(row.dataReportEnabled ? '已启用数据上报' : '已禁用数据上报')
  } catch (error) {
    // 恢复开关状态
    row.dataReportEnabled = !row.dataReportEnabled
    ElMessage.error('操作失败')
  } finally {
    row.updating = false
  }
}

// 配置上报
const handleConfigReport = (row) => {
  selectedDevice.value = row
  reportConfigDialogVisible.value = true
}

// 查看日志
const handleViewLog = (row) => {
  router.push(`/devices/${row.id}/report-logs`)
}

// 配置成功回调
const handleConfigSuccess = () => {
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.device-list {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.search-form {
  margin-bottom: 20px;
}
</style>
