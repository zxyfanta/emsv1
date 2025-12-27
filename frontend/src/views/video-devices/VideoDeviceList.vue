<template>
  <div class="video-device-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>视频设备管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            添加视频设备
          </el-button>
        </div>
      </template>

      <!-- 搜索框 -->
      <el-input
        v-model="searchQuery"
        placeholder="搜索设备编码/名称"
        prefix-icon="Search"
        clearable
        style="width: 300px; margin-bottom: 16px"
        @input="handleSearch"
      />

      <!-- 表格 -->
      <el-table :data="filteredDevices" v-loading="loading" stripe>
        <!-- 设备编码 -->
        <el-table-column prop="deviceCode" label="设备编码" width="150" />

        <!-- 设备名称 -->
        <el-table-column prop="deviceName" label="设备名称" width="180" />

        <!-- 流类型 -->
        <el-table-column prop="streamType" label="流类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.streamType }}</el-tag>
          </template>
        </el-table-column>

        <!-- 企业名称（仅管理员可见） -->
        <el-table-column
          v-if="userStore.isAdmin"
          prop="companyName"
          label="所属企业"
          width="200"
        />

        <!-- 绑定的监测设备 -->
        <el-table-column label="绑定设备" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.linkedDevice" type="success" size="small">
              {{ row.linkedDevice.deviceCode }}
            </el-tag>
            <el-tag v-else type="info" size="small">未绑定</el-tag>
          </template>
        </el-table-column>

        <!-- 分辨率 -->
        <el-table-column prop="resolution" label="分辨率" width="120" />

        <!-- 帧率 -->
        <el-table-column prop="fps" label="帧率" width="80">
          <template #default="{ row }">
            <span v-if="row.fps">{{ row.fps }} fps</span>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="success" @click="handleBind(row)">
              {{ row.linkedDevice ? '重新绑定' : '绑定' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页（仅管理员需要分页，普通用户数据量小不分页） -->
      <el-pagination
        v-if="userStore.isAdmin"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 20px"
      />
    </el-card>

    <!-- 创建/编辑对话框 -->
    <VideoDeviceFormDialog
      v-model="formDialogVisible"
      :video-device="currentVideoDevice"
      @success="handleFormSuccess"
    />

    <!-- 绑定对话框 -->
    <VideoDeviceBindDialog
      v-model="bindDialogVisible"
      :video-device="currentVideoDevice"
      @success="handleBindSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import {
  getAllVideoDevices,
  getVideoDevices,
  deleteVideoDevice
} from '@/api/video'
import VideoDeviceFormDialog from './VideoDeviceFormDialog.vue'
import VideoDeviceBindDialog from './VideoDeviceBindDialog.vue'

const userStore = useUserStore()
const loading = ref(false)
const searchQuery = ref('')
const devices = ref([])

// 分页（仅管理员使用）
const pagination = ref({
  page: 0,
  size: 10,
  total: 0
})

// 对话框控制
const formDialogVisible = ref(false)
const bindDialogVisible = ref(false)
const currentVideoDevice = ref(null)

// 过滤设备
const filteredDevices = computed(() => {
  if (!searchQuery.value) {
    return devices.value
  }
  const query = searchQuery.value.toLowerCase()
  return devices.value.filter(device =>
    device.deviceCode?.toLowerCase().includes(query) ||
    device.deviceName?.toLowerCase().includes(query)
  )
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    let res
    if (userStore.isAdmin) {
      // 管理员：分页加载所有视频设备
      const params = {
        page: pagination.value.page,
        size: pagination.value.size
      }
      res = await getVideoDevices(params.page, params.size)
      if (res.status === 200) {
        devices.value = res.data.content
        pagination.value.total = res.data.totalElements
      }
    } else {
      // 普通用户：加载本企业的所有视频设备（不分页）
      res = await getAllVideoDevices()
      if (res.status === 200) {
        devices.value = res.data
      }
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 创建
const handleCreate = () => {
  currentVideoDevice.value = null
  formDialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  currentVideoDevice.value = row
  formDialogVisible.value = true
}

// 绑定
const handleBind = (row) => {
  currentVideoDevice.value = row
  bindDialogVisible.value = true
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除视频设备 "${row.deviceName}" 吗？`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteVideoDevice(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 搜索
const handleSearch = () => {
  // 过滤逻辑由computed自动处理
}

// 表单提交成功
const handleFormSuccess = () => {
  formDialogVisible.value = false
  loadData()
}

// 绑定成功
const handleBindSuccess = () => {
  bindDialogVisible.value = false
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.video-device-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
