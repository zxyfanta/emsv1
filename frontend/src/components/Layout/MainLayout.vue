<template>
  <el-container class="main-layout">
    <el-aside :width="sidebarWidth">
      <AppSidebar />
    </el-aside>
    <el-container>
      <el-header height="60px">
        <AppHeader />
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import AppHeader from './AppHeader.vue'
import AppSidebar from './AppSidebar.vue'

const appStore = useAppStore()
const userStore = useUserStore()
const sidebarWidth = computed(() => appStore.sidebarCollapsed ? '64px' : '200px')

// 加载系统配置
onMounted(async () => {
  if (userStore.isLoggedIn) {
    await appStore.fetchSystemConfig()
  }
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.el-aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
}

.el-header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
