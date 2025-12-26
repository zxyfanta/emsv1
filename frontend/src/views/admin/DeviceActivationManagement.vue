<template>
  <div class="activation-management-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>设备激活管理</h2>
          <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 待激活设备 -->
        <el-tab-pane label="待激活设备" name="pending">
          <el-alert
            title="待激活设备包括通过批量导入生成激活码的设备，以及其他方式创建的待激活设备"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 15px"
          />

          <el-alert
            v-if="pendingDevices.length === 0"
            title="暂无待激活设备"
            type="info"
            :closable="false"
            show-icon
          />

          <el-table
            v-else
            :data="pendingDevices"
            border
            stripe
            v-loading="loading"
          >
            <el-table-column prop="id" label="ID" width="60" />

            <el-table-column prop="deviceCode" label="设备编码" width="120" />

            <el-table-column label="设备类型" width="150">
              <template #default="{ row }">
                {{ getDeviceTypeName(row.deviceType) }}
              </template>
            </el-table-column>

            <el-table-column prop="serialNumber" label="序列号" width="150" />

            <el-table-column label="激活码" width="200">
              <template #default="{ row }">
                <el-tag type="info" style="font-family: monospace; font-size: 13px">
                  {{ row.activationCode }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="manufacturer" label="制造商" width="120" />

            <el-table-column prop="model" label="型号" width="120" />

            <el-table-column label="生成时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.generatedAt) }}
              </template>
            </el-table-column>

            <el-table-column label="有效期至" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.expiresAt) }}
              </template>
            </el-table-column>

            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.activationCode && row.activationCode !== '无激活码'"
                  type="primary"
                  size="small"
                  @click="copyCode(row.activationCode)"
                >
                  复制激活码
                </el-button>
                <el-tag v-else type="info" size="small">无激活码</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="pendingDevices.length > 0" class="table-footer">
            <span>共 {{ pendingDevices.length }} 台待激活设备</span>
          </div>
        </el-tab-pane>

        <!-- 已激活设备 -->
        <el-tab-pane label="已激活设备" name="activated">
          <el-alert
            title="已激活设备包括所有状态为已激活的设备，无论是否通过激活码激活"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 15px"
          />

          <el-alert
            v-if="activatedDevices.length === 0"
            title="暂无已激活设备"
            type="info"
            :closable="false"
            show-icon
          />

          <el-table
            v-else
            :data="activatedDevices"
            border
            stripe
            v-loading="loading"
          >
            <el-table-column prop="id" label="ID" width="60" />

            <el-table-column prop="deviceCode" label="设备编码" width="120" />

            <el-table-column label="设备类型" width="150">
              <template #default="{ row }">
                {{ getDeviceTypeName(row.deviceType) }}
              </template>
            </el-table-column>

            <el-table-column prop="serialNumber" label="序列号" width="150" />

            <el-table-column prop="company" label="归属企业" width="150" />

            <el-table-column prop="activatedBy" label="激活用户" width="120" />

            <el-table-column label="激活时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.activatedAt) }}
              </template>
            </el-table-column>

            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  link
                  @click="viewDevice(row.id)"
                >
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="activatedDevices.length > 0" class="table-footer">
            <span>共 {{ activatedDevices.length }} 台已激活设备</span>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getDeviceList } from '@/api/device'

const router = useRouter()

const activeTab = ref('pending')
const loading = ref(false)
const pendingDevices = ref([])
const activatedDevices = ref([])

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'pending') {
      await loadPendingDevices()
    } else {
      await loadActivatedDevices()
    }
  } finally {
    loading.value = false
  }
}

// 加载待激活设备
const loadPendingDevices = async () => {
  try {
    // 使用统一的设备查询接口，按激活状态过滤
    const res = await getDeviceList({ activationStatus: 'PENDING', size: 1000 })
    if (res.status === 200) {
      // 处理数据：添加激活码信息（如果有）
      pendingDevices.value = (res.data.content || []).map(device => ({
        id: device.id,
        deviceCode: device.deviceCode,
        deviceType: device.deviceType,
        serialNumber: device.serialNumber,
        manufacturer: device.manufacturer,
        model: device.model,
        activationCode: device.activationCode || '无激活码',  // 显示激活码（如果有）
        generatedAt: device.createdAt,  // 使用创建时间作为生成时间
        expiresAt: null  // 设备实体没有过期时间字段
      }))
    }
  } catch (error) {
    console.error('加载待激活设备失败:', error)
    ElMessage.error('加载待激活设备失败')
  }
}

// 加载已激活设备
const loadActivatedDevices = async () => {
  try {
    // 使用统一的设备查询接口，按激活状态过滤
    const res = await getDeviceList({ activationStatus: 'ACTIVE', size: 1000 })
    if (res.status === 200) {
      // 处理数据结构
      activatedDevices.value = (res.data.content || []).map(device => ({
        id: device.id,
        deviceCode: device.deviceCode,
        deviceType: device.deviceType,
        serialNumber: device.serialNumber,
        company: device.company?.companyName || '未分配',
        activatedBy: device.updatedBy || '未知',  // 使用更新人作为激活人
        activatedAt: device.installDate || device.createdAt  // 使用安装时间或创建时间
      }))
    }
  } catch (error) {
    console.error('加载已激活设备失败:', error)
    ElMessage.error('加载已激活设备失败')
  }
}

// 标签页切换
const handleTabChange = (tabName) => {
  loadData()
}

// 复制激活码
const copyCode = async (code) => {
  try {
    await navigator.clipboard.writeText(code)
    ElMessage.success('激活码已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败，请手动复制')
  }
}

// 查看设备详情（跳转到管理员设备列表页面）
const viewDevice = (id) => {
  router.push('/admin/devices/list')
}

// 获取设备类型名称
const getDeviceTypeName = (type) => {
  const typeMap = {
    'RADIATION_MONITOR': '辐射监测设备',
    'ENVIRONMENT_STATION': '环境监测站'
  }
  return typeMap[type] || type
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 初始化
onMounted(() => {
  loadData()
})
</script>

<style scoped>
.activation-management-container {
  padding: 20px;
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

.table-footer {
  margin-top: 15px;
  text-align: right;
  color: #606266;
  font-size: 14px;
}
</style>
