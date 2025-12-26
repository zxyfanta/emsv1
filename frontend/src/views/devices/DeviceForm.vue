<template>
  <div class="device-form">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑设备' : '手动录入设备' }}</span>
          <el-button @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        v-loading="loading"
      >
        <el-form-item label="设备编码" prop="deviceCode">
          <el-input
            v-model="form.deviceCode"
            placeholder="请输入设备编码"
            :disabled="isEdit"
          />
        </el-form-item>

        <el-form-item label="设备名称" prop="deviceName">
          <el-input v-model="form.deviceName" placeholder="请输入设备名称" />
        </el-form-item>

        <el-form-item label="设备类型" prop="deviceType">
          <el-select
            v-model="form.deviceType"
            placeholder="请选择设备类型"
            :disabled="isEdit"
            style="width: 100%"
          >
            <el-option label="辐射监测仪" value="RADIATION_MONITOR" />
            <el-option label="环境监测站" value="ENVIRONMENT_STATION" />
          </el-select>
        </el-form-item>

        <el-form-item label="设备位置" prop="location">
          <el-input v-model="form.location" placeholder="请输入设备位置" />
        </el-form-item>

        <el-form-item label="设备描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入设备描述"
          />
        </el-form-item>

        <el-divider content-position="left">可视化大屏位置</el-divider>

        <el-form-item label="X坐标 (0-100)" prop="positionX">
          <el-slider
            v-model="form.positionX"
            :min="0"
            :max="100"
            :step="1"
            show-input
            :marks="{ 0: '左', 50: '中', 100: '右' }"
          />
        </el-form-item>

        <el-form-item label="Y坐标 (0-100)" prop="positionY">
          <el-slider
            v-model="form.positionY"
            :min="0"
            :max="100"
            :step="1"
            show-input
            :marks="{ 0: '下', 50: '中', 100: '上' }"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            保存
          </el-button>
          <el-button @click="handleBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  getDeviceDetail,
  createDevice,
  updateDevice
} from '@/api/device'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 管理员权限检查
const isAdmin = computed(() => userStore.isAdmin)

const formRef = ref()
const loading = ref(false)
const submitting = ref(false)

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  deviceCode: '',
  deviceName: '',
  deviceType: '',
  location: '',
  description: '',
  positionX: null,
  positionY: null
})

const rules = {
  deviceCode: [
    { required: true, message: '请输入设备编码', trigger: 'blur' }
  ],
  deviceName: [
    { required: true, message: '请输入设备名称', trigger: 'blur' }
  ],
  deviceType: [
    { required: true, message: '请选择设备类型', trigger: 'change' }
  ]
}

const loadDeviceDetail = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  try {
    const res = await getDeviceDetail(id)
    if (res.status === 200) {
      Object.assign(form, res.data)
    }
  } catch (error) {
    ElMessage.error('加载设备信息失败')
    handleBack()
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEdit.value) {
        await updateDevice(route.params.id, form)
        ElMessage.success('更新成功')

        // 触发自定义事件，通知其他页面刷新设备数据
        window.dispatchEvent(new CustomEvent('device-updated', {
          detail: {
            deviceId: route.params.id,
            type: 'updated'
          }
        }))
        console.log('[设备编辑] 已触发设备更新事件')
      } else {
        await createDevice(form)
        ElMessage.success('创建成功')

        // 触发自定义事件，通知其他页面刷新设备数据
        window.dispatchEvent(new CustomEvent('device-updated', {
          detail: {
            type: 'created'
          }
        }))
        console.log('[设备创建] 已触发设备更新事件')
      }
      handleBack()
    } catch (error) {
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    } finally {
      submitting.value = false
    }
  })
}

const handleBack = () => {
  router.back()
}

onMounted(() => {
  // 权限检查：非管理员不能手动录入设备
  if (!isAdmin.value && !isEdit.value) {
    ElMessage.warning('您没有权限手动录入设备，请使用激活功能')
    router.push('/devices/activate')
    return
  }

  if (isEdit.value) {
    loadDeviceDetail()
  }
})
</script>

<style scoped>
.device-form {
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
