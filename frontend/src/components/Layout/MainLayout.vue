<template>
  <el-container class="main-layout">
    <el-aside v-show="!appStore.isFullscreen" :width="sidebarWidth">
      <AppSidebar />
    </el-aside>
    <el-container>
      <el-header v-show="!appStore.isFullscreen" height="60px">
        <AppHeader />
      </el-header>
      <el-main :class="{ 'fullscreen-main': appStore.isFullscreen }">
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
  height: 100%;
  overflow: hidden;
}

/* 全屏模式样式 */
.el-main.fullscreen-main {
  padding: 0 !important;
  height: 100vh !important;
  background-color: transparent !important;
  overflow: hidden !important;
}

/* 全屏模式下隐藏内部容器的边距和背景 */
:deep(.fullscreen-main .root-container) {
  height: 100vh !important;
}

/* 当处于全屏状态时，让 el-container 也占满整个视口 */
.main-layout:has(.fullscreen-main) {
  height: 100vh;
  overflow: hidden;
}

.main-layout:has(.fullscreen-main) .el-container {
  height: 100vh;
}
</style>
