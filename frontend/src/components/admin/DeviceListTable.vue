<template>
  <div class="device-list-table">
    <el-table
      :data="tableData"
      v-loading="loading"
      stripe
      border
    >
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
          <el-tag v-if="row.status === 'ONLINE'" type="success">在线</el-tag>
          <el-tag v-else-if="row.status === 'OFFLINE'" type="info">离线</el-tag>
          <el-tag v-else-if="row.status === 'MAINTENANCE'" type="warning">维护中</el-tag>
          <el-tag v-else type="danger">故障</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="activationStatus" label="激活状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.activationStatus === 'ACTIVE'" type="success">已激活</el-tag>
          <el-tag v-else type="warning">待激活</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="location" label="位置" min-width="120" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handlePageChange"
      style="margin-top: 20px; justify-content: center"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeviceList, deleteDevice } from '@/api/device'

const props = defineProps({
  showAll: {
    type: Boolean,
    default: false
  },
  activationStatus: {
    type: String,
    default: null
  }
})

const router = useRouter()
const loading = ref(false)
const tableData = ref([])

const pagination = ref({
  page: 0,
  size: 10,
  total: 0
})

// 构建查询参数
const queryParams = computed(() => {
  const params = {
    page: pagination.value.page,
    size: pagination.value.size
  }

  // 如果指定了激活状态，则过滤
  if (props.activationStatus) {
    params.activationStatus = props.activationStatus
  }

  return params
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getDeviceList(queryParams.value)
    if (res.status === 200) {
      tableData.value = res.data.content
      pagination.value.total = res.data.totalElements
    }
  } catch (error) {
    console.error('加载设备列表失败:', error)
    ElMessage.error('加载设备列表失败')
  } finally {
    loading.value = false
  }
}

// 查看设备详情
const handleView = (row) => {
  // 显示设备详情对话框或跳转到详情页
  // 这里暂时使用对话框显示基本信息
  ElMessageBox.alert(
    `
    <div style="text-align: left;">
      <p><strong>设备编码:</strong> ${row.deviceCode}</p>
      <p><strong>设备名称:</strong> ${row.deviceName}</p>
      <p><strong>设备类型:</strong> ${row.deviceType === 'RADIATION_MONITOR' ? '辐射监测仪' : '环境监测站'}</p>
      <p><strong>状态:</strong> ${row.status}</p>
      <p><strong>激活状态:</strong> ${row.activationStatus}</p>
      <p><strong>位置:</strong> ${row.location || '未设置'}</p>
      <p><strong>制造商:</strong> ${row.manufacturer || '未设置'}</p>
      <p><strong>型号:</strong> ${row.model || '未设置'}</p>
      <p><strong>序列号:</strong> ${row.serialNumber || '未设置'}</p>
      <p><strong>描述:</strong> ${row.description || '无'}</p>
      ${row.installDate ? `<p><strong>安装日期:</strong> ${new Date(row.installDate).toLocaleString()}</p>` : ''}
      ${row.lastOnlineAt ? `<p><strong>最后在线:</strong> ${new Date(row.lastOnlineAt).toLocaleString()}</p>` : ''}
    </div>
    `,
    '设备详情',
    {
      dangerouslyUseHTMLString: true,
      confirmButtonText: '关闭'
    }
  )
}

// 编辑设备
const handleEdit = (row) => {
  router.push(`/devices/${row.id}/edit`)
}

// 删除设备
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除设备 "${row.deviceName}" 吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loading.value = true
    const res = await deleteDevice(row.id)

    if (res.status === 200 || res.status === 204) {
      ElMessage.success('删除成功')
      // 重新加载数据
      await loadData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除设备失败:', error)
      ElMessage.error('删除设备失败')
    }
  } finally {
    loading.value = false
  }
}

const handleSizeChange = (size) => {
  pagination.value.size = size
  pagination.value.page = 0
  loadData()
}

const handlePageChange = (page) => {
  pagination.value.page = page - 1
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.device-list-table {
  min-height: 400px;
}

:deep(.el-pagination) {
  display: flex;
}

:deep(.el-message-box__content) {
  max-height: 400px;
  overflow-y: auto;
}
</style>
