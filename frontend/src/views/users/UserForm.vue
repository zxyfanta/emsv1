<template>
  <div class="user-form">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑用户' : '添加用户' }}</span>
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
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="formData.username"
            placeholder="请输入用户名"
            :disabled="isEdit"
          />
        </el-form-item>

        <el-form-item label="姓名" prop="fullName">
          <el-input
            v-model="formData.fullName"
            placeholder="请输入姓名"
          />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="formData.email"
            placeholder="请输入邮箱"
            type="email"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="formData.role">
            <el-radio value="USER">普通用户</el-radio>
            <el-radio value="ADMIN">管理员</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="所属企业" prop="companyId">
          <el-select
            v-model="formData.companyId"
            placeholder="请选择企业"
            style="width: 100%"
            :loading="companiesLoading"
          >
            <el-option
              v-for="company in companies"
              :key="company.id"
              :label="company.companyName"
              :value="company.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio value="ACTIVE">活跃</el-radio>
            <el-radio value="INACTIVE">非活跃</el-radio>
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
import { getUserDetail, createUser, updateUser } from '@/api/user'
import { getCompanyList } from '@/api/company'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const companiesLoading = ref(false)

const isEdit = computed(() => !!route.params.id)

const formData = reactive({
  username: '',
  fullName: '',
  email: '',
  password: '',
  role: 'USER',
  companyId: null,
  status: 'ACTIVE'
})

const companies = ref([])

const rules = computed(() => {
  const baseRules = {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 3, max: 50, message: '长度在 3 到 50 个字符', trigger: 'blur' }
    ],
    fullName: [
      { required: true, message: '请输入姓名', trigger: 'blur' },
      { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
    ],
    email: [
      { required: true, message: '请输入邮箱', trigger: 'blur' },
      { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
    ],
    role: [
      { required: true, message: '请选择角色', trigger: 'change' }
    ],
    companyId: [
      { required: true, message: '请选择所属企业', trigger: 'change' }
    ],
    status: [
      { required: true, message: '请选择状态', trigger: 'change' }
    ]
  }

  // 只有创建时才需要密码验证
  if (!isEdit.value) {
    baseRules.password = [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
    ]
  }

  return baseRules
})

// 加载企业列表
const loadCompanies = async () => {
  companiesLoading.value = true
  try {
    const res = await getCompanyList({ size: 1000 })
    if (res.status === 200) {
      companies.value = res.data.content
    }
  } catch (error) {
    console.error('加载企业列表失败:', error)
    ElMessage.error('加载企业列表失败')
  } finally {
    companiesLoading.value = false
  }
}

// 加载用户详情（编辑模式）
const loadUserDetail = async () => {
  if (!isEdit.value) return

  loading.value = true
  try {
    const res = await getUserDetail(route.params.id)
    if (res.status === 200) {
      Object.assign(formData, res.data)
      // 处理 companyId（如果有 company 对象）
      if (res.data.company && res.data.company.id) {
        formData.companyId = res.data.company.id
      }
    }
  } catch (error) {
    console.error('加载用户详情失败:', error)
    ElMessage.error('加载用户详情失败')
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

    // 准备提交数据
    const submitData = {
      username: formData.username,
      fullName: formData.fullName,
      email: formData.email,
      role: formData.role,
      companyId: formData.companyId,
      status: formData.status
    }

    // 只有创建时才包含密码
    if (!isEdit.value) {
      submitData.password = formData.password
    }

    if (isEdit.value) {
      // 更新用户
      const res = await updateUser(route.params.id, submitData)
      if (res.status === 200) {
        ElMessage.success('更新成功')
        handleBack()
      }
    } else {
      // 创建用户
      const res = await createUser(submitData)
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
  router.push('/users')
}

onMounted(() => {
  loadCompanies()
  loadUserDetail()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-form {
  max-width: 800px;
  margin: 0 auto;
}
</style>
