<template>
  <el-dialog
    :model-value="modelValue"
    title="绑定监测设备"
    width="500px"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div v-if="videoDevice">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="视频设备">
          {{ videoDevice.deviceName }} ({{ videoDevice.deviceCode }})
        </el-descriptions-item>
        <el-descriptions-item label="当前绑定">
          <el-tag v-if="videoDevice.linkedDevice" type="success">
            {{ videoDevice.linkedDevice.deviceCode }}
          </el-tag>
          <el-tag v-else type="info">未绑定</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <el-form v-if="!videoDevice.linkedDevice" label-width="100px">
        <el-form-item label="选择设备">
          <el-select
            v-model="selectedDeviceId"
            placeholder="请选择要绑定的监测设备"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="device in availableDevices"
              :key="device.id"
              :label="`${device.deviceName} (${device.deviceCode})`"
              :value="device.id"
            />
          </el-select>
          <div class="form-tip">
            仅显示本企业的监测设备
          </div>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="videoDevice.linkedDevice"
        title="设备已绑定，如需重新绑定请先解绑当前设备"
        type="info"
        :closable="false"
      />
    </div>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button
        v-if="videoDevice?.linkedDevice"
        type="danger"
        @click="handleUnbind"
        :loading="submitting"
      >
        解绑
      </el-button>
      <el-button
        v-else
        type="primary"
        @click="handleBind"
        :loading="submitting"
        :disabled="!selectedDeviceId"
      >
        绑定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  bindVideoToDevice,
  unbindVideoDevice
} from '@/api/video'
import { getDeviceList } from '@/api/device'

const props = defineProps({
  modelValue: Boolean,
  videoDevice: Object
})

const emit = defineEmits(['update:modelValue', 'success'])

const submitting = ref(false)
const selectedDeviceId = ref(null)
const availableDevices = ref([])

// 加载可用的监测设备
const loadAvailableDevices = async () => {
  try {
    // 获取本企业的所有监测设备
    const res = await getDeviceList({ page: 0, size: 1000 })
    if (res.status === 200) {
      availableDevices.value = res.data.content || []
    }
  } catch (error) {
    ElMessage.error('加载监测设备失败')
  }
}

const handleBind = async () => {
  if (!selectedDeviceId.value) {
    ElMessage.warning('请选择要绑定的监测设备')
    return
  }

  try {
    submitting.value = true
    await bindVideoToDevice(props.videoDevice.id, selectedDeviceId.value)
    ElMessage.success('绑定成功')
    emit('success')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '绑定失败')
  } finally {
    submitting.value = false
  }
}

const handleUnbind = async () => {
  try {
    submitting.value = true
    await unbindVideoDevice(props.videoDevice.id)
    ElMessage.success('解绑成功')
    emit('success')
  } catch (error) {
    ElMessage.error('解绑失败')
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  emit('update:modelValue', false)
}

watch(() => props.modelValue, (newVal) => {
  if (newVal && !props.videoDevice?.linkedDevice) {
    loadAvailableDevices()
  }
  if (!newVal) {
    selectedDeviceId.value = null
  }
})
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 4px;
}
</style>
