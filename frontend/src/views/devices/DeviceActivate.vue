<template>
  <div class="device-activate-container">
    <el-card class="activate-card">
      <template #header>
        <div class="card-header">
          <h2>激活设备</h2>
          <el-steps :active="step" simple>
            <el-step title="输入激活码" />
            <el-step title="完善信息" />
          </el-steps>
        </div>
      </template>

      <!-- 步骤1：输入激活码 -->
      <div v-if="step === 1" class="step-content">
        <el-alert
          title="请输入管理员提供的设备激活码"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        />

        <el-form ref="codeFormRef" :model="codeForm" :rules="codeRules" label-width="120px">
          <el-form-item label="激活码" prop="activationCode">
            <el-input
              v-model="codeForm.activationCode"
              placeholder="请输入激活码，格式：EMS-RAD-XXXXXXXX 或 EMS-ENV-XXXXXXXX"
              clearable
              @blur="handleVerifyCode"
            >
              <template #append>
                <el-button :icon="Search" @click="handleVerifyCode">验证</el-button>
              </template>
            </el-input>
          </el-form-item>

          <!-- 验证成功后显示设备信息 -->
          <el-alert
            v-if="codeInfo"
            title="激活码验证成功"
            type="success"
            :closable="false"
            show-icon
            style="margin-top: 20px"
          >
            <template #default>
              <div class="device-info">
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="设备编码">{{ codeInfo.deviceCode }}</el-descriptions-item>
                  <el-descriptions-item label="设备类型">{{ getDeviceTypeName(codeInfo.deviceType) }}</el-descriptions-item>
                  <el-descriptions-item label="序列号">{{ codeInfo.serialNumber }}</el-descriptions-item>
                  <el-descriptions-item label="制造商">{{ codeInfo.manufacturer }}</el-descriptions-item>
                  <el-descriptions-item label="型号">{{ codeInfo.model }}</el-descriptions-item>
                  <el-descriptions-item label="生产日期">{{ formatDate(codeInfo.productionDate) }}</el-descriptions-item>
                  <el-descriptions-item label="有效期至">{{ formatDate(codeInfo.expiresAt) }}</el-descriptions-item>
                </el-descriptions>
              </div>
            </template>
          </el-alert>

          <el-alert
            v-if="verifyError"
            :title="verifyError"
            type="error"
            :closable="false"
            show-icon
            style="margin-top: 20px"
          />
        </el-form>

        <div class="step-actions">
          <el-button type="primary" :disabled="!codeInfo" @click="nextStep">下一步</el-button>
        </div>
      </div>

      <!-- 步骤2：完善设备信息 -->
      <div v-if="step === 2" class="step-content">
        <el-alert
          title="请完善设备的安装信息"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
        />

        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
          <el-form-item label="设备名称" prop="deviceName">
            <el-input
              v-model="form.deviceName"
              placeholder="请输入设备名称"
              clearable
            />
          </el-form-item>

          <el-form-item label="安装位置" prop="location">
            <el-input
              v-model="form.location"
              placeholder="请输入安装位置，如：办公区A栋"
              clearable
            />
          </el-form-item>

          <el-form-item label="X坐标">
            <el-input-number
              v-model="form.positionX"
              :min="0"
              :max="100"
              placeholder="0-100"
              controls-position="right"
            />
            <span class="form-tip">用于3D可视化场景定位（可选）</span>
          </el-form-item>

          <el-form-item label="Y坐标">
            <el-input-number
              v-model="form.positionY"
              :min="0"
              :max="100"
              placeholder="0-100"
              controls-position="right"
            />
            <span class="form-tip">用于3D可视化场景定位（可选）</span>
          </el-form-item>

          <el-form-item label="备注说明">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="请输入设备备注说明（可选）"
            />
          </el-form-item>
        </el-form>

        <div class="step-actions">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" :loading="loading" @click="handleActivate">激活设备</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { verifyActivationCode, activateDevice } from '@/api/device'

const router = useRouter()

// 步骤控制
const step = ref(1)
const loading = ref(false)

// 激活码表单
const codeForm = reactive({
  activationCode: ''
})

const codeRules = {
  activationCode: [
    { required: true, message: '请输入激活码', trigger: 'blur' },
    {
      pattern: /^EMS-(RAD|ENV)-[A-Z0-9]{8}$/,
      message: '激活码格式不正确，应为：EMS-RAD-XXXXXXXX 或 EMS-ENV-XXXXXXXX',
      trigger: 'blur'
    }
  ]
}

// 设备信息表单
const form = reactive({
  deviceName: '',
  location: '',
  positionX: null,
  positionY: null,
  description: ''
})

const rules = {
  deviceName: [
    { required: true, message: '请输入设备名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  location: [
    { required: true, message: '请输入安装位置', trigger: 'blur' }
  ]
}

// 激活码验证结果
const codeInfo = ref(null)
const verifyError = ref('')

// 验证激活码
const handleVerifyCode = async () => {
  if (!codeForm.activationCode) {
    return
  }

  try {
    verifyError.value = ''
    const res = await verifyActivationCode(codeForm.activationCode)
    if (res.status === 200) {
      codeInfo.value = res.data
      ElMessage.success('激活码验证成功')
      // 预填充设备名称
      if (!form.deviceName) {
        form.deviceName = res.data.deviceCode
      }
    } else {
      verifyError.value = res.message || '激活码验证失败'
    }
  } catch (error) {
    verifyError.value = error.response?.data?.message || '激活码验证失败，请检查激活码是否正确'
    codeInfo.value = null
  }
}

// 下一步
const nextStep = () => {
  step.value = 2
}

// 上一步
const prevStep = () => {
  step.value = 1
}

// 激活设备
const handleActivate = async () => {
  try {
    loading.value = true
    const res = await activateDevice({
      activationCode: codeForm.activationCode,
      deviceName: form.deviceName,
      location: form.location,
      positionX: form.positionX,
      positionY: form.positionY,
      description: form.description
    })

    if (res.status === 200) {
      ElMessage.success('设备激活成功！')
      setTimeout(() => {
        router.push('/devices/list')
      }, 1500)
    } else {
      ElMessage.error(res.message || '设备激活失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '设备激活失败')
  } finally {
    loading.value = false
  }
}

// 获取设备类型名称
const getDeviceTypeName = (type) => {
  const typeMap = {
    'RADIATION_MONITOR': '辐射监测设备',
    'ENVIRONMENT_STATION': '环境监测站'
  }
  return typeMap[type] || type
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}
</script>

<style scoped>
.device-activate-container {
  padding: 20px;
  max-width: 900px;
  margin: 0 auto;
}

.activate-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

/* 确保步骤条在同一行 */
:deep(.el-steps) {
  display: flex;
  width: 100%;
  flex: 1;
}

:deep(.el-step) {
  flex: 1;
  min-width: 120px;
}

/* 确保步骤标题不换行 */
:deep(.el-step__title) {
  white-space: nowrap;
}

.step-content {
  padding: 20px 0;
}

.device-info {
  margin-top: 15px;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.step-actions {
  margin-top: 30px;
  text-align: center;
}

.step-actions .el-button {
  margin: 0 10px;
  min-width: 120px;
}

/* 确保表单标签不换行 */
:deep(.el-form-item__label) {
  white-space: nowrap;
}

/* 确保表单项标签和输入框在同一行 */
:deep(.el-form-item) {
  display: flex;
  align-items: center;
}

:deep(.el-form-item__content) {
  flex: 1;
  min-width: 0;
}
</style>
