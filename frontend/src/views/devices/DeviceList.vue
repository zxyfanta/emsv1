<template>
  <div class="device-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>设备列表</span>
          <el-button type="primary" @click="handleCreate" v-if="isAdmin">
            <el-icon><Plus /></el-icon>
            添加设备
          </el-button>
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
        <el-table-column prop="deviceCode" label="设备编码" width="150" />
        <el-table-column prop="deviceName" label="设备名称" width="180" />
        <el-table-column prop="deviceType" label="设备类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.deviceType === 'RADIATION_MONITOR'" type="warning">辐射监测仪</el-tag>
            <el-tag v-else type="success">环境监测站</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" />
        <el-table-column prop="lastOnlineAt" label="最后在线时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.lastOnlineAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button link type="primary" @click="handleEdit(row)" v-if="isAdmin">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)" v-if="isAdmin">删除</el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getDeviceList, deleteDevice as deleteDeviceApi } from '@/api/device'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.isAdmin)

const loading = ref(false)
const tableData = ref([])

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

const handleCreate = () => {
  router.push('/devices/create')
}

const handleView = (row) => {
  // TODO: 显示设备详情对话框
  ElMessage.info('查看功能待实现')
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

.search-form {
  margin-bottom: 20px;
}
</style>
