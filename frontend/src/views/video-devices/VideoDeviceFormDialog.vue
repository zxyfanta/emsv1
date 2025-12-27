<template>
  <el-dialog
    :model-value="modelValue"
    :title="isEdit ? '编辑视频设备' : '添加视频设备'"
    width="500px"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <el-form-item label="设备名称" prop="deviceName">
        <el-input
          v-model="form.deviceName"
          placeholder="请输入设备名称"
        />
      </el-form-item>

      <el-form-item label="IP地址" prop="ipAddress">
        <el-input
          v-model="form.ipAddress"
          placeholder="例如：192.168.1.100"
          @input="updateStreamUrl"
        />
        <div class="form-tip">
          系统将自动生成RTSP流地址：rtsp://IP:554/stream
        </div>
      </el-form-item>

      <el-form-item label="认证用户名" prop="username">
        <el-input
          v-model="form.username"
          placeholder="请输入认证用户名（可选）"
        />
      </el-form-item>

      <el-form-item label="认证密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入认证密码（可选）"
          show-password
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createVideoDevice,
  updateVideoDevice
} from '@/api/video'

const props = defineProps({
  modelValue: Boolean,
  videoDevice: Object
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref(null)
const submitting = ref(false)

const isEdit = computed(() => !!props.videoDevice)

const form = ref({
  deviceCode: '',
  deviceName: '',
  streamUrl: '',
  streamType: 'RTSP',
  username: '',
  password: ''
})

const rules = {
  deviceName: [
    { required: true, message: '请输入设备名称', trigger: 'blur' }
  ],
  ipAddress: [
    { required: true, message: '请输入IP地址', trigger: 'blur' },
    {
      pattern: /^(\d{1,3}\.){3}\d{1,3}$/,
      message: '请输入有效的IP地址',
      trigger: 'blur'
    }
  ]
}

// 生成设备编码
const generateDeviceCode = () => {
  const timestamp = Date.now().toString().slice(-6)
  return `VIDEO-${timestamp}`
}

// 根据IP地址更新streamUrl
const updateStreamUrl = () => {
  if (form.value.ipAddress) {
    form.value.streamUrl = `rtsp://${form.value.ipAddress}:554/stream`
  }
}

const resetForm = () => {
  form.value = {
    deviceCode: '',
    deviceName: '',
    streamUrl: '',
    streamType: 'RTSP',
    username: '',
    password: ''
  }
  formRef.value?.clearValidate()
}

// 监听 videoDevice 变化，填充表单
watch(() => props.videoDevice, (newVal) => {
  if (newVal) {
    // 从streamUrl中提取IP地址
    let ipAddress = ''
    try {
      const url = newVal.streamUrl || ''
      const match = url.match(/rtsp:\/\/([\d.]+)/)
      if (match) {
        ipAddress = match[1]
      }
    } catch (e) {
      console.warn('解析IP地址失败', e)
    }

    form.value = {
      deviceCode: newVal.deviceCode || '',
      deviceName: newVal.deviceName || '',
      streamUrl: newVal.streamUrl || '',
      streamType: 'RTSP',
      username: newVal.username || '',
      password: '', // 密码不回填
      ipAddress: ipAddress
    }
  } else {
    resetForm()
  }
}, { immediate: true })

const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    // 自动生成设备编码（仅创建时）
    if (!isEdit.value && !form.value.deviceCode) {
      form.value.deviceCode = generateDeviceCode()
    }

    // 确保streamUrl已生成
    if (!form.value.streamUrl && form.value.ipAddress) {
      updateStreamUrl()
    }

    submitting.value = true

    if (isEdit.value) {
      await updateVideoDevice(props.videoDevice.id, form.value)
      ElMessage.success('更新成功')
    } else {
      await createVideoDevice(form.value)
      ElMessage.success('创建成功')
    }

    emit('success')
  } catch (error) {
    if (error !== false) { // 表单验证失败时error为false
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    }
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 4px;
}
</style>
