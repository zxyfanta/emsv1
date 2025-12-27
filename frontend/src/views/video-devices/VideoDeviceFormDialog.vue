<template>
  <el-dialog
    :model-value="modelValue"
    :title="isEdit ? '编辑视频设备' : '添加视频设备'"
    width="600px"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item label="设备编码" prop="deviceCode">
        <el-input
          v-model="form.deviceCode"
          placeholder="请输入设备编码（唯一）"
          :disabled="isEdit"
        />
      </el-form-item>

      <el-form-item label="设备名称" prop="deviceName">
        <el-input
          v-model="form.deviceName"
          placeholder="请输入设备名称"
        />
      </el-form-item>

      <el-form-item label="视频流URL" prop="streamUrl">
        <el-input
          v-model="form.streamUrl"
          placeholder="例如：rtsp://192.168.1.100:554/stream"
        />
        <div class="form-tip">
          支持 RTSP、RTMP、HLS、FLV、WebRTC 等协议
        </div>
      </el-form-item>

      <el-form-item label="流类型" prop="streamType">
        <el-select v-model="form.streamType" placeholder="请选择流类型" style="width: 100%">
          <el-option label="RTSP" value="RTSP" />
          <el-option label="RTMP" value="RTMP" />
          <el-option label="HLS" value="HLS" />
          <el-option label="FLV" value="FLV" />
          <el-option label="WebRTC" value="WEBRTC" />
        </el-select>
      </el-form-item>

      <el-form-item label="截图URL" prop="snapshotUrl">
        <el-input
          v-model="form.snapshotUrl"
          placeholder="请输入截图URL（可选）"
        />
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

      <el-form-item label="分辨率" prop="resolution">
        <el-input
          v-model="form.resolution"
          placeholder="例如：1920x1080（可选）"
        />
      </el-form-item>

      <el-form-item label="帧率" prop="fps">
        <el-input-number
          v-model="form.fps"
          :min="1"
          :max="60"
          placeholder="帧率（可选）"
          style="width: 100%"
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
  snapshotUrl: '',
  username: '',
  password: '',
  resolution: '',
  fps: null
})

const rules = {
  deviceCode: [
    { required: true, message: '请输入设备编码', trigger: 'blur' }
  ],
  deviceName: [
    { required: true, message: '请输入设备名称', trigger: 'blur' }
  ],
  streamUrl: [
    { required: true, message: '请输入视频流URL', trigger: 'blur' },
    {
      pattern: /^(rtsp|rtmp|http|https):\/\/.+/i,
      message: '请输入有效的URL',
      trigger: 'blur'
    }
  ],
  streamType: [
    { required: true, message: '请选择流类型', trigger: 'change' }
  ]
}

// 监听 videoDevice 变化，填充表单
watch(() => props.videoDevice, (newVal) => {
  if (newVal) {
    form.value = {
      deviceCode: newVal.deviceCode || '',
      deviceName: newVal.deviceName || '',
      streamUrl: newVal.streamUrl || '',
      streamType: newVal.streamType || 'RTSP',
      snapshotUrl: newVal.snapshotUrl || '',
      username: newVal.username || '',
      password: '', // 密码不回填
      resolution: newVal.resolution || '',
      fps: newVal.fps || null
    }
  } else {
    resetForm()
  }
}, { immediate: true })

const resetForm = () => {
  form.value = {
    deviceCode: '',
    deviceName: '',
    streamUrl: '',
    streamType: 'RTSP',
    snapshotUrl: '',
    username: '',
    password: '',
    resolution: '',
    fps: null
  }
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
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
