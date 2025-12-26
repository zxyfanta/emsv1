<template>
  <div class="batch-import-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>批量导入设备</h2>
          <el-button type="primary" :icon="Plus" @click="addDeviceRow">添加设备</el-button>
        </div>
      </template>

      <el-alert
        title="批量录入设备信息，系统将自动生成激活码"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      >
        <template #default>
          <div>提示：设备编码和序列号必须唯一，重复的设备将被自动跳过</div>
        </template>
      </el-alert>

      <!-- 设备列表表格 -->
      <el-table
        :data="deviceList"
        border
        stripe
        style="margin-bottom: 20px"
        max-height="500"
      >
        <el-table-column type="index" label="序号" width="60" />

        <el-table-column label="设备编码" min-width="150">
          <template #default="{ row }">
            <el-input
              v-model="row.deviceCode"
              placeholder="如：RAD001"
              clearable
            />
          </template>
        </el-table-column>

        <el-table-column label="设备类型" width="180">
          <template #default="{ row }">
            <el-select v-model="row.deviceType" placeholder="选择类型">
              <el-option
                label="辐射监测设备"
                value="RADIATION_MONITOR"
              />
              <el-option
                label="环境监测站"
                value="ENVIRONMENT_STATION"
              />
            </el-select>
          </template>
        </el-table-column>

        <el-table-column label="序列号" min-width="150">
          <template #default="{ row }">
            <el-input
              v-model="row.serialNumber"
              placeholder="设备唯一序列号"
              clearable
            />
          </template>
        </el-table-column>

        <el-table-column label="制造商" min-width="120">
          <template #default="{ row }">
            <el-input
              v-model="row.manufacturer"
              placeholder="制造商"
              clearable
            />
          </template>
        </el-table-column>

        <el-table-column label="型号" min-width="120">
          <template #default="{ row }">
            <el-input
              v-model="row.model"
              placeholder="设备型号"
              clearable
            />
          </template>
        </el-table-column>

        <el-table-column label="生产日期" width="180">
          <template #default="{ row }">
            <el-date-picker
              v-model="row.productionDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ $index }">
            <el-button
              type="danger"
              :icon="Delete"
              circle
              size="small"
              @click="removeRow($index)"
            />
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <el-button @click="clearAll">清空列表</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">
          批量导入 ({{ deviceList.length }}台)
        </el-button>
      </div>
    </el-card>

    <!-- 导入结果 -->
    <el-card v-if="importResult" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <h3>导入结果</h3>
          <el-tag :type="importResult.importedCount > 0 ? 'success' : 'info'">
            成功 {{ importResult.importedCount }} / 总数 {{ importResult.totalCount }}
          </el-tag>
        </div>
      </template>

      <el-alert
        v-if="importResult.importedCount === 0"
        title="没有设备导入成功，请检查数据格式和必填字段"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      />

      <div v-if="importResult.importedCount > 0">
        <h4>已生成的激活码</h4>
        <el-alert
          title="请将激活码提供给客户进行设备激活"
          type="success"
          :closable="false"
          show-icon
          style="margin-bottom: 15px"
        />

        <el-table
          :data="importResult.activationCodes"
          border
          stripe
          max-height="400"
        >
          <el-table-column prop="code" label="激活码" min-width="180">
            <template #default="{ row }">
              <el-tag type="info">{{ row.code }}</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="deviceCode" label="设备编码" width="120" />

          <el-table-column prop="deviceType" label="设备类型" width="150">
            <template #default="{ row }">
              {{ getDeviceTypeName(row.deviceType) }}
            </template>
          </el-table-column>

          <el-table-column prop="serialNumber" label="序列号" width="150" />

          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                @click="copyCode(row.code)"
              >
                复制
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="result-actions">
          <el-button @click="importResult = null">关闭结果</el-button>
          <el-button type="success" @click="copyAllCodes">
            复制全部激活码
          </el-button>
          <el-button type="primary" @click="refreshPage">
            继续导入
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { batchImportDevices } from '@/api/device'

// 设备列表
const deviceList = ref([])
const importing = ref(false)
const importResult = ref(null)

// 添加设备行
const addDeviceRow = () => {
  deviceList.value.push({
    deviceCode: '',
    deviceType: 'RADIATION_MONITOR',
    serialNumber: '',
    manufacturer: '',
    model: '',
    productionDate: ''
  })
}

// 删除行
const removeRow = (index) => {
  deviceList.value.splice(index, 1)
}

// 清空列表
const clearAll = () => {
  ElMessageBox.confirm(
    '确定要清空所有设备吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    deviceList.value = []
    ElMessage.success('已清空')
  }).catch(() => {})
}

// 验证表单
const validateForm = () => {
  if (deviceList.value.length === 0) {
    ElMessage.warning('请至少添加一台设备')
    return false
  }

  for (let i = 0; i < deviceList.value.length; i++) {
    const item = deviceList.value[i]
    if (!item.deviceCode) {
      ElMessage.warning(`第 ${i + 1} 行：设备编码不能为空`)
      return false
    }
    if (!item.deviceType) {
      ElMessage.warning(`第 ${i + 1} 行：设备类型不能为空`)
      return false
    }
    if (!item.serialNumber) {
      ElMessage.warning(`第 ${i + 1} 行：序列号不能为空`)
      return false
    }
    if (!item.manufacturer) {
      ElMessage.warning(`第 ${i + 1} 行：制造商不能为空`)
      return false
    }
    if (!item.model) {
      ElMessage.warning(`第 ${i + 1} 行：型号不能为空`)
      return false
    }
  }

  return true
}

// 批量导入
const handleImport = async () => {
  if (!validateForm()) {
    return
  }

  try {
    importing.value = true
    // 处理日期格式：将 2025-12-26 转换为 2025-12-26T00:00:00
    const processedList = deviceList.value.map(item => ({
      ...item,
      productionDate: item.productionDate ? `${item.productionDate}T00:00:00` : null
    }))
    const res = await batchImportDevices(processedList)

    if (res.code === 200) {
      importResult.value = res.data
      ElMessage.success(`导入完成！成功 ${res.data.importedCount} 台`)
    } else {
      ElMessage.error(res.message || '导入失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '导入失败')
  } finally {
    importing.value = false
  }
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

// 复制全部激活码
const copyAllCodes = async () => {
  if (!importResult.value?.activationCodes) return

  const codes = importResult.value.activationCodes
    .map(item => `${item.code}\t${item.deviceCode}\t${item.serialNumber}`)
    .join('\n')

  try {
    await navigator.clipboard.writeText(codes)
    ElMessage.success('全部激活码已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败，请手动复制')
  }
}

// 刷新页面
const refreshPage = () => {
  deviceList.value = []
  importResult.value = null
  // 添加默认行
  addDeviceRow()
}

// 获取设备类型名称
const getDeviceTypeName = (type) => {
  const typeMap = {
    'RADIATION_MONITOR': '辐射监测设备',
    'ENVIRONMENT_STATION': '环境监测站'
  }
  return typeMap[type] || type
}

// 初始化：添加一行默认数据
addDeviceRow()
</script>

<style scoped>
.batch-import-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2,
.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.table-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
}

.result-actions {
  margin-top: 20px;
  text-align: center;
}

.result-actions .el-button {
  margin: 0 10px;
}
</style>
