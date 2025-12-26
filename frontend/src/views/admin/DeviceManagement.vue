<template>
  <div class="admin-device-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>管理员设备管理</h2>
          <div class="header-actions">
            <el-button type="primary" :icon="Plus" @click="navigateToBatchImport">
              批量导入
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        title="管理员可查看所有设备（包括待激活和已激活），并进行统一管理"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      />

      <!-- Tab页签 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="所有设备" name="all">
          <DeviceListTable ref="allDeviceTable" :show-all="true" />
        </el-tab-pane>

        <el-tab-pane label="待激活设备" name="pending">
          <DeviceListTable ref="pendingDeviceTable" :activation-status="'PENDING'" />
        </el-tab-pane>

        <el-tab-pane label="已激活设备" name="activated">
          <DeviceListTable ref="activatedDeviceTable" :activation-status="'ACTIVE'" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Upload } from '@element-plus/icons-vue'
import DeviceListTable from '@/components/admin/DeviceListTable.vue'

const router = useRouter()
const activeTab = ref('all')

// 导航到批量导入
const navigateToBatchImport = () => {
  router.push('/admin/devices/batch-import')
}

// Tab切换
const handleTabChange = (tabName) => {
  // 刷新对应表格数据
  if (tabName === 'all') {
    // 刷新所有设备表格
  } else if (tabName === 'pending') {
    // 刷新待激活设备表格
  } else if (tabName === 'activated') {
    // 刷新已激活设备表格
  }
}
</script>

<style scoped>
.admin-device-management {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.device-table-container {
  min-height: 400px;
}
</style>
