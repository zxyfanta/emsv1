<template>
  <el-dialog
    :model-value="modelValue"
    :title="`配置上报 - ${device?.deviceName || ''}`"
    width="700px"
    @update:model-value="$emit('update:modelValue', $event)"
    :close-on-click-modal="false"
  >
    <el-form
      v-if="device"
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="140px"
    >
      <!-- ====== 基础配置 ====== -->
      <el-divider content-position="left">基础配置</el-divider>

      <el-form-item label="启用数据上报">
        <el-switch v-model="form.dataReportEnabled" />
        <span class="ml-2 text-gray-500 text-sm">
          开启后将自动上报数据到监管平台
        </span>
      </el-form-item>

      <el-form-item label="上报协议" prop="reportProtocol">
        <el-radio-group
          v-model="form.reportProtocol"
          :disabled="!form.dataReportEnabled"
          @change="handleProtocolChange"
        >
          <el-radio value="SICHUAN" border class="protocol-radio">
            <div class="protocol-option">
              <div class="protocol-title">四川协议</div>
              <div class="protocol-desc">HTTP + SM2加密</div>
              <div class="protocol-req">需配置：核素类型</div>
            </div>
          </el-radio>
          <el-radio value="SHANDONG" border class="protocol-radio">
            <div class="protocol-option">
              <div class="protocol-title">山东协议</div>
              <div class="protocol-desc">TCP + HJ/T212-2005</div>
              <div class="protocol-req">需配置：放射源信息（7项）</div>
            </div>
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- GPS说明 -->
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="mb-4"
      >
        <template #title>
          <span>📍 GPS自动选择</span>
        </template>
        <p class="mb-2">系统会根据设备上报数据自动选择最优GPS：</p>
        <ul class="ml-4 mb-0">
          <li>北斗GPS可用（useful=1）→ 使用北斗</li>
          <li>北斗不可用 → 使用基站GPS</li>
        </ul>
      </el-alert>

      <!-- ====== 辐射设备专用配置 ====== -->
      <div v-if="device.deviceType === 'RADIATION_MONITOR' && form.dataReportEnabled">
        <el-divider content-position="left">
          辐射设备配置
        </el-divider>

        <!-- 四川协议字段 -->
        <template v-if="form.reportProtocol === 'SICHUAN'">
          <el-form-item label="核素类型" prop="nuclide">
            <el-input
              v-model="form.nuclide"
              placeholder="如：Cs-137、Co-60"
              clearable
            >
              <template #prefix>🧪</template>
            </el-input>
            <div class="text-gray text-sm mt-1">
              示例：Cs-137、Co-60、I-125
            </div>
          </el-form-item>
        </template>

        <!-- 山东协议字段 -->
        <template v-if="form.reportProtocol === 'SHANDONG'">
          <el-form-item label="探伤机编号" prop="inspectionMachineNumber">
            <el-input
              v-model="form.inspectionMachineNumber"
              placeholder="6位数字"
              maxlength="6"
              clearable
            >
              <template #prefix>🔧</template>
            </el-input>
          </el-form-item>

          <el-form-item label="放射源编号" prop="sourceNumber">
            <el-input
              v-model="form.sourceNumber"
              placeholder="12位数字"
              maxlength="12"
              clearable
            >
              <template #prefix>☢️</template>
            </el-input>
          </el-form-item>

          <el-form-item label="放射源类别" prop="sourceType">
            <el-select v-model="form.sourceType" placeholder="请选择" style="width: 100%">
              <el-option label="Ⅰ类" value="01" />
              <el-option label="Ⅱ类" value="02" />
              <el-option label="Ⅲ类" value="03" />
              <el-option label="Ⅳ类" value="04" />
              <el-option label="Ⅴ类" value="05" />
            </el-select>
          </el-form-item>

          <el-form-item label="原始活度" prop="originalActivity">
            <el-input
              v-model="form.originalActivity"
              placeholder="如：2.700E004"
              clearable
            >
              <template #prefix>📊</template>
            </el-input>
            <div class="text-gray text-sm mt-1">
              科学计数法格式，如：2.700E004
            </div>
          </el-form-item>

          <el-form-item label="当前活度" prop="currentActivity">
            <el-input
              v-model="form.currentActivity"
              placeholder="如：1.300E004"
              clearable
            >
              <template #prefix>📈</template>
            </el-input>
          </el-form-item>

          <el-form-item label="出厂日期" prop="sourceProductionDate">
            <el-date-picker
              v-model="form.sourceProductionDate"
              type="date"
              placeholder="选择日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </template>
      </div>

      <!-- 环境设备提示 -->
      <div v-if="device.deviceType === 'ENVIRONMENT_STATION' && form.dataReportEnabled">
        <el-alert type="success" :closable="false" show-icon>
          环境设备无需配置额外参数
        </el-alert>
      </div>

    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          保存配置
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { updateDevice } from '@/api/device'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  device: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref()
const submitting = ref(false)

// 表单数据
const form = reactive({
  dataReportEnabled: false,
  reportProtocol: 'SICHUAN',
  nuclide: null,
  inspectionMachineNumber: null,
  sourceNumber: null,
  sourceType: null,
  originalActivity: null,
  currentActivity: null,
  sourceProductionDate: null
})

// 表单验证规则
const rules = computed(() => {
  const baseRules = {
    reportProtocol: [
      { required: true, message: '请选择上报协议', trigger: 'change' }
    ]
  }

  // 如果未启用上报，只验证协议选择
  if (!form.dataReportEnabled) {
    return baseRules
  }

  // 四川协议验证
  if (form.reportProtocol === 'SICHUAN') {
    return {
      ...baseRules,
      nuclide: [
        { required: true, message: '请输入核素类型', trigger: 'blur' }
      ]
    }
  }

  // 山东协议验证
  if (form.reportProtocol === 'SHANDONG') {
    return {
      ...baseRules,
      inspectionMachineNumber: [
        { required: true, message: '请输入探伤机编号', trigger: 'blur' },
        { pattern: /^\d{6}$/, message: '必须为6位数字', trigger: 'blur' }
      ],
      sourceNumber: [
        { required: true, message: '请输入放射源编号', trigger: 'blur' },
        { pattern: /^\d{12}$/, message: '必须为12位数字', trigger: 'blur' }
      ],
      sourceType: [
        { required: true, message: '请选择放射源类别', trigger: 'change' }
      ],
      originalActivity: [
        { required: true, message: '请输入原始活度', trigger: 'blur' },
        { pattern: /^\d+\.\d+E[+-]?\d+$/, message: '科学计数法格式，如2.700E004', trigger: 'blur' }
      ],
      currentActivity: [
        { required: true, message: '请输入当前活度', trigger: 'blur' },
        { pattern: /^\d+\.\d+E[+-]?\d+$/, message: '科学计数法格式，如1.300E004', trigger: 'blur' }
      ],
      sourceProductionDate: [
        { required: true, message: '请选择出厂日期', trigger: 'change' }
      ]
    }
  }

  return baseRules
})

// 监听设备变化，初始化表单
watch(() => props.device, (newDevice) => {
  if (newDevice) {
    form.dataReportEnabled = newDevice.dataReportEnabled || false
    form.reportProtocol = newDevice.reportProtocol || 'SICHUAN'
    form.nuclide = newDevice.nuclide || null
    form.inspectionMachineNumber = newDevice.inspectionMachineNumber || null
    form.sourceNumber = newDevice.sourceNumber || null
    form.sourceType = newDevice.sourceType || null
    form.originalActivity = newDevice.originalActivity || null
    form.currentActivity = newDevice.currentActivity || null
    form.sourceProductionDate = newDevice.sourceProductionDate || null
  }
}, { immediate: true })

// 协议切换处理
const handleProtocolChange = (newProtocol) => {
  if (newProtocol === 'SICHUAN') {
    // 设置默认值
    form.nuclide = form.nuclide || 'Cs-137'
    // 清空山东协议字段
    form.inspectionMachineNumber = null
    form.sourceNumber = null
    form.sourceType = null
    form.originalActivity = null
    form.currentActivity = null
    form.sourceProductionDate = null
  } else if (newProtocol === 'SHANDONG') {
    // 设置默认值
    form.sourceType = form.sourceType || '01'
    // 清空四川协议字段
    form.nuclide = null
  }
  // 清除验证错误
  formRef.value?.clearValidate()
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()

    submitting.value = true
    const data = {
      dataReportEnabled: form.dataReportEnabled,
      reportProtocol: form.reportProtocol
    }

    // 仅在启用上报时添加协议特定字段
    if (form.dataReportEnabled) {
      if (form.reportProtocol === 'SICHUAN') {
        data.nuclide = form.nuclide
      } else if (form.reportProtocol === 'SHANDONG') {
        data.inspectionMachineNumber = form.inspectionMachineNumber
        data.sourceNumber = form.sourceNumber
        data.sourceType = form.sourceType
        data.originalActivity = form.originalActivity
        data.currentActivity = form.currentActivity
        data.sourceProductionDate = form.sourceProductionDate
      }
    }

    await updateDevice(props.device.id, data)
    ElMessage.success('保存成功')
    emit('success')
    handleClose()
  } catch (error) {
    if (error !== false) { // 排除表单验证错误
      ElMessage.error('保存失败')
    }
  } finally {
    submitting.value = false
  }
}

// 关闭对话框
const handleClose = () => {
  emit('update:modelValue', false)
  formRef.value?.resetFields()
}
</script>

<style scoped>
.protocol-radio {
  display: flex;
  margin-bottom: 10px;
  width: 100%;
  height: auto;
  padding: 10px;
}

.protocol-radio :deep(.el-radio__input) {
  line-height: 20px;
}

.protocol-radio :deep(.el-radio__label) {
  width: 100%;
  padding-left: 10px;
  line-height: 20px;
}

.protocol-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

.protocol-title {
  font-weight: bold;
  font-size: 14px;
  line-height: 20px;
}

.protocol-desc {
  font-size: 12px;
  color: #606266;
  line-height: 18px;
}

.protocol-req {
  font-size: 12px;
  color: #909399;
  line-height: 18px;
}

.text-gray {
  color: #909399;
}

.text-sm {
  font-size: 12px;
}

.ml-2 {
  margin-left: 8px;
}

.mb-2 {
  margin-bottom: 8px;
}

.mb-4 {
  margin-bottom: 16px;
}

.ml-4 {
  margin-left: 16px;
}

.mt-1 {
  margin-top: 4px;
}
</style>
