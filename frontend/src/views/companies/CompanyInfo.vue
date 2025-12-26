<template>
  <div class="company-info">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>企业信息</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
        v-loading="loading"
      >
        <el-form-item label="企业名称" prop="companyName">
          <el-input
            v-model="formData.companyName"
            placeholder="请输入企业名称"
          />
        </el-form-item>

        <el-form-item label="联系邮箱" prop="contactEmail">
          <el-input
            v-model="formData.contactEmail"
            placeholder="请输入联系邮箱"
            type="email"
          />
        </el-form-item>

        <el-form-item label="联系电话" prop="contactPhone">
          <el-input
            v-model="formData.contactPhone"
            placeholder="请输入联系电话"
          />
        </el-form-item>

        <el-form-item label="地址" prop="address">
          <el-input
            v-model="formData.address"
            type="textarea"
            :rows="3"
            placeholder="请输入地址"
          />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入企业描述"
          />
        </el-form-item>

        <el-form-item label="用户数">
          <el-tag>{{ userCount }}</el-tag>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            保存
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentCompanyInfo, updateCurrentCompany } from '@/api/company'

const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const userCount = ref(0)

const formData = reactive({
  companyName: '',
  contactEmail: '',
  contactPhone: '',
  address: '',
  description: ''
})

const rules = {
  companyName: [
    { required: true, message: '请输入企业名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  contactEmail: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  contactPhone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
}

// 加载企业信息
const loadCompanyInfo = async () => {
  loading.value = true
  try {
    const res = await getCurrentCompanyInfo()
    if (res.status === 200) {
      Object.assign(formData, {
        companyName: res.data.companyName,
        contactEmail: res.data.contactEmail,
        contactPhone: res.data.contactPhone,
        address: res.data.address,
        description: res.data.description
      })
      userCount.value = res.data.userCount || 0
    }
  } catch (error) {
    console.error('加载企业信息失败:', error)
    ElMessage.error('加载企业信息失败')
  } finally {
    loading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    submitting.value = true

    const res = await updateCurrentCompany(formData)
    if (res.status === 200) {
      ElMessage.success('更新成功')
      await loadCompanyInfo() // 重新加载数据
    }
  } catch (error) {
    if (error !== false) {
      console.error('提交失败:', error)
      ElMessage.error('更新失败')
    }
  } finally {
    submitting.value = false
  }
}

// 重置表单
const handleReset = () => {
  loadCompanyInfo()
}

onMounted(() => {
  loadCompanyInfo()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.company-info {
  max-width: 800px;
  margin: 0 auto;
}
</style>
