<template>
  <div class="user-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            添加用户
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="fullName" label="姓名" width="150" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="companyName" label="所属企业" width="200" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">
              {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
        style="margin-top: 20px"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getUserList, deleteUser as deleteUserApi, resetUserPassword } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])

const pagination = reactive({
  page: 0,
  size: 10,
  total: 0
})

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }
    const res = await getUserList(params)
    if (res.status === 200) {
      tableData.value = res.data.content
      pagination.total = res.data.totalElements
    }
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  ElMessage.info('添加用户功能待实现')
}

const handleEdit = (row) => {
  ElMessage.info('编辑功能待实现')
}

const handleResetPassword = (row) => {
  ElMessageBox.prompt('请输入新密码', `重置用户 ${row.username} 的密码`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^.{6,}$/,
    inputErrorMessage: '密码长度不能少于6位'
  }).then(async ({ value }) => {
    try {
      await resetUserPassword(row.id, value)
      ElMessage.success('密码重置成功')
    } catch (error) {
      ElMessage.error('密码重置失败')
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除用户 "${row.username}" 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteUserApi(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
