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
            <el-button type="success" :icon="Key" @click="navigateToActivation">
              激活码管理
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

      <!-- 快捷操作卡片 -->
      <el-row :gutter="20" style="margin-bottom: 20px">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card" @click="navigateToBatchImport">
            <div class="stat-content">
              <el-icon class="stat-icon" :size="40"><Upload /></el-icon>
              <div class="stat-text">
                <div class="stat-value">{{ stats.pendingCount }}</div>
                <div class="stat-label">待激活设备</div>
              </div>
            </div>
            <div class="stat-action">批量导入设备 →</div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover" class="stat-card" @click="navigateToActivation">
            <div class="stat-content">
              <el-icon class="stat-icon" :size="40"><Key /></el-icon>
              <div class="stat-text">
                <div class="stat-value">{{ stats.activationCodeCount }}</div>
                <div class="stat-label">激活码数量</div>
              </div>
            </div>
            <div class="stat-action">管理激活码 →</div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-content">
              <el-icon class="stat-icon" :size="40"><CircleCheck /></el-icon>
              <div class="stat-text">
                <div class="stat-value">{{ stats.activatedCount }}</div>
                <div class="stat-label">已激活设备</div>
              </div>
            </div>
            <div class="stat-action">系统统计</div>
          </el-card>
        </el-col>
      </el-row>

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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Upload, Key, CircleCheck } from '@element-plus/icons-vue'
import { getDeviceStatistics } from '@/api/device'

const router = useRouter()
const activeTab = ref('all')

const stats = reactive({
  pendingCount: 0,
  activatedCount: 0,
  activationCodeCount: 0
})

// 加载统计数据
const loadStatistics = async () => {
  try {
    const response = await getDeviceStatistics()
    if (response.data) {
      stats.pendingCount = response.data.pendingCount || 0
      stats.activatedCount = response.data.activatedCount || 0
      stats.activationCodeCount = response.data.activationCodeCount || 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 导航到批量导入
const navigateToBatchImport = () => {
  router.push('/admin/devices/batch-import')
}

// 导航到激活码管理
const navigateToActivation = () => {
  router.push('/admin/devices/activation')
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

onMounted(() => {
  loadStatistics()
})
</script>

<script setup>
// 为了简化，这里暂时使用一个简单的表格组件占位
// 实际应该复用 DeviceList.vue 中的表格逻辑
const DeviceListTable = {
  name: 'DeviceListTable',
  props: {
    showAll: Boolean,
    activationStatus: String
  },
  template: `
    <div class="device-table-container">
      <el-alert
        title="表格功能复用自 DeviceList.vue，请根据需要显示对应设备列表"
        type="info"
        :closable="false"
        show-icon
      />
      <div style="margin-top: 20px; text-align: center; color: #909399;">
        设备列表表格组件（待实现）
      </div>
    </div>
  `
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

.stat-card {
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-icon {
  color: #409eff;
}

.stat-text {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.stat-action {
  margin-top: 10px;
  font-size: 13px;
  color: #409eff;
  text-align: right;
}

.device-table-container {
  min-height: 400px;
}
</style>
