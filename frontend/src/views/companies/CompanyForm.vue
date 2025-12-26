<template>
  <div class="company-form">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑企业' : '添加企业' }}</span>
          <el-button @click="handleBack">返回</el-button>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
        v-loading="loading"
      >
        <el-form-item label="企业编码" prop="companyCode">
          <el-input
            v-model="formData.companyCode"
            placeholder="请输入企业编码"
            :disabled="isEdit"
          />
        </el-form-item>

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

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio label="ACTIVE">活跃</el-radio>
            <el-radio label="INACTIVE">非活跃</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="handleBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCompanyDetail, createCompany, updateCompany } from '@/api/company'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)

const isEdit = computed(() => !!route.params.id)

const formData = reactive({
  companyCode: '',
  companyName: '',
  contactEmail: '',
  contactPhone: '',
  address: '',
  description: '',
  status: 'ACTIVE'
})

const rules = {
  companyCode: [
    { required: true, message: '请输入企业编码', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  companyName: [
    { required: true, message: '请输入企业名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  contactEmail: [
    { required: true, message: '请输入联系邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 加载企业详情（编辑模式）
const loadCompanyDetail = async () => {
  if (!isEdit.value) return

  loading.value = true
  try {
    const res = await getCompanyDetail(route.params.id)
    if (res.status === 200) {
      Object.assign(formData, res.data)
    }
  } catch (error) {
    console.error('加载企业详情失败:', error)
    ElMessage.error('加载企业详情失败')
    handleBack()
  } finally {
    loading.value = false
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    submitting.value = true

    if (isEdit.value) {
      // 更新企业
      const res = await updateCompany(route.params.id, formData)
      if (res.status === 200) {
        ElMessage.success('更新成功')
        handleBack()
      }
    } else {
      // 创建企业
      const res = await createCompany(formData)
      if (res.status === 200 || res.status === 201) {
        ElMessage.success('创建成功')
        handleBack()
      }
    }
  } catch (error) {
    if (error !== false) { // 排除表单验证错误
      console.error('提交失败:', error)
      ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
    }
  } finally {
    submitting.value = false
  }
}

// 重置表单
const handleReset = () => {
  formRef.value.resetFields()
}

// 返回列表
const handleBack = () => {
  router.push('/companies')
}

onMounted(() => {
  loadCompanyDetail()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.company-form {
  max-width: 800px;
  margin: 0 auto;
}
</style>
