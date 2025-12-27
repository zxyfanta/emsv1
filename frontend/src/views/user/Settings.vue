<template>
  <div class="user-settings">
    <el-card>
      <template #header>
        <span>个人设置</span>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="120px"
            style="max-width: 600px"
          >
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input
                v-model="passwordForm.oldPassword"
                type="password"
                placeholder="请输入当前密码"
                show-password
                clearable
              />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码"
                show-password
                clearable
              />
              <div class="password-strength">
                <span>密码强度：</span>
                <span :class="['strength-' + passwordStrength]">{{ strengthText }}</span>
              </div>
            </el-form-item>

            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
                clearable
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">
                修改密码
              </el-button>
              <el-button @click="resetPasswordForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 个人信息 -->
        <el-tab-pane label="个人信息" name="profile">
          <el-form
            ref="profileFormRef"
            :model="profileForm"
            :rules="profileRules"
            label-width="120px"
            style="max-width: 600px"
          >
            <el-form-item label="用户名">
              <el-input v-model="userStore.username" disabled />
            </el-form-item>

            <el-form-item label="姓名" prop="fullName">
              <el-input
                v-model="profileForm.fullName"
                placeholder="请输入姓名"
                clearable
              />
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input
                v-model="profileForm.email"
                placeholder="请输入邮箱"
                clearable
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="profileLoading" @click="handleUpdateProfile">
                保存修改
              </el-button>
              <el-button @click="resetProfileForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import { changePassword, updateCurrentProfile } from '@/api/user'

const userStore = useUserStore()
const activeTab = ref('password')

// 修改密码表单
const passwordFormRef = ref(null)
const passwordLoading = ref(false)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 密码强度检测
const passwordStrength = computed(() => {
  const password = passwordForm.newPassword
  if (!password) return 0

  let strength = 0
  if (password.length >= 8) strength++
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++
  if (/\d/.test(password)) strength++
  if (/[^a-zA-Z0-9]/.test(password)) strength++

  return Math.min(strength, 3)
})

const strengthText = computed(() => {
  const texts = ['弱', '一般', '强', '很强']
  return texts[passwordStrength.value] || '弱'
})

// 修改密码
const handleChangePassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })

    ElMessage.success('密码修改成功，请重新登录')
    resetPasswordForm()

    // 延迟跳转到登录页
    setTimeout(() => {
      userStore.logout()
      window.location.href = '/login'
    }, 1500)
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '密码修改失败')
  } finally {
    passwordLoading.value = false
  }
}

const resetPasswordForm = () => {
  passwordFormRef.value?.resetFields()
}

// 个人信息表单
const profileFormRef = ref(null)
const profileLoading = ref(false)
const profileForm = reactive({
  fullName: '',
  email: ''
})

const profileRules = {
  fullName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 更新个人信息
const handleUpdateProfile = async () => {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return

  profileLoading.value = true
  try {
    const res = await updateCurrentProfile(profileForm)

    if (res.status === 200) {
      // 更新本地用户信息
      userStore.updateUserInfo(res.data)
      ElMessage.success('个人信息更新成功')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '更新失败')
  } finally {
    profileLoading.value = false
  }
}

const resetProfileForm = () => {
  profileForm.fullName = userStore.userInfo?.fullName || ''
  profileForm.email = userStore.userInfo?.email || ''
}

onMounted(() => {
  resetProfileForm()
})
</script>

<style scoped>
.user-settings {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.password-strength {
  margin-top: 8px;
  font-size: 14px;
  color: #606266;
}

.strength-0 {
  color: #F56C6C;
}

.strength-1 {
  color: #E6A23C;
}

.strength-2 {
  color: #409EFF;
}

.strength-3 {
  color: #67C23A;
  font-weight: bold;
}
</style>
