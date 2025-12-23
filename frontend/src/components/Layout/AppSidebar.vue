<template>
  <div class="app-sidebar">
    <div class="logo-container" v-if="!appStore.sidebarCollapsed">
      <h2>EMS系统</h2>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="appStore.sidebarCollapsed"
      :unique-opened="true"
      router
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
    >
      <template v-for="route in menuRoutes" :key="route.path">
        <el-menu-item
          v-if="!route.meta?.hidden && hasPermission(route)"
          :index="route.path"
          :route="route.path"
        >
          <el-icon v-if="route.meta?.icon">
            <component :is="getIcon(route.meta.icon)" />
          </el-icon>
          <template #title>{{ route.meta.title }}</template>
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useUserStore } from '@/store/user'
import * as ElementPlusIcons from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const activeMenu = computed(() => {
  const { path } = route
  if (path.startsWith('/devices/')) {
    return '/devices'
  }
  return path
})

const menuRoutes = computed(() => {
  const mainRoute = router.getRoutes().find(r => r.path === '/')
  const children = mainRoute?.children || []

  // 在菜单顶部添加可视化大屏（指向独立路由）
  const visualizationRoute = router.getRoutes().find(r => r.path === '/visualization')
  if (visualizationRoute) {
    return [visualizationRoute, ...children]
  }

  return children
})

/**
 * 检查路由是否可显示
 * 1. 首先检查角色权限
 * 2. 然后检查系统功能开关配置
 */
const hasPermission = (route) => {
  const { systemConfig } = appStore

  // 1. 检查角色权限
  const roles = route.meta?.roles
  if (roles && !roles.includes(userStore.userRole)) {
    return false
  }

  // 2. 检查系统功能开关
  const path = route.path.slice(1) // 去掉开头的 /
  const moduleCode = path.split('/')[0]

  if (moduleCode === 'devices') {
    // 设备管理：辐射或环境任一启用即显示
    return systemConfig.radiationEnabled || systemConfig.environmentEnabled
  }

  if (moduleCode === 'radiation-data') {
    return systemConfig.radiationEnabled
  }

  if (moduleCode === 'environment-data') {
    return systemConfig.environmentEnabled
  }

  return true
}

const getIcon = (iconName) => {
  return ElementPlusIcons[iconName] || ElementPlusIcons.Document
}
</script>

<style scoped>
.app-sidebar {
  height: 100%;
  overflow: hidden;
}

.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3a4a;
}

.logo-container h2 {
  margin: 0;
  color: #fff;
  font-size: 20px;
}

.el-menu {
  border-right: none;
  height: calc(100% - 60px);
}
</style>
