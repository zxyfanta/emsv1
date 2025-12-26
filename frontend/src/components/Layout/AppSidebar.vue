<template>
  <div class="app-sidebar">
    <div class="logo-container" v-if="!appStore.sidebarCollapsed">
      <h2>EMS系统</h2>
    </div>
    <el-menu
      :default-active="activeMenu"
      :default-openeds="defaultOpenedMenus"
      :collapse="appStore.sidebarCollapsed"
      :unique-opened="false"
      router
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
    >
      <!-- 可视化大屏 -->
      <template v-for="route in menuRoutes" :key="route.path">
        <!-- 有子菜单的路由 -->
        <el-sub-menu v-if="hasChildren(route) && !route.meta?.hidden && hasPermission(route)" :index="route.path">
          <template #title>
            <el-icon v-if="route.meta?.icon">
              <component :is="getIcon(route.meta.icon)" />
            </el-icon>
            <span>{{ route.meta?.title }}</span>
          </template>
          <!-- 子菜单项 -->
          <template v-for="child in getChildren(route.path)" :key="child.path">
            <el-menu-item
              v-if="!child.meta?.hidden && hasPermission(child)"
              :index="child.path"
              :route="child.path"
            >
              <el-icon v-if="child.meta?.icon">
                <component :is="getIcon(child.meta.icon)" />
              </el-icon>
              <template #title>{{ child.meta?.title }}</template>
            </el-menu-item>
          </template>
        </el-sub-menu>

        <!-- 无子菜单的路由 -->
        <el-menu-item
          v-else-if="!route.meta?.hidden && hasPermission(route)"
          :index="route.path"
          :route="route.path"
        >
          <el-icon v-if="route.meta?.icon">
            <component :is="getIcon(route.meta.icon)" />
          </el-icon>
          <template #title>{{ route.meta?.title }}</template>
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

// 当前激活的菜单
const activeMenu = computed(() => {
  const { path } = route

  // 可视化大屏单独处理
  if (path === '/visualization') {
    return '/visualization'
  }

  // 设备管理相关路由统一映射到主菜单
  if (path.startsWith('/devices/') && path !== '/devices/list') {
    return '/devices'
  }

  // 管理员设备管理路由 - 返回当前路径因为使用子菜单
  if (path.startsWith('/admin/devices/')) {
    return path
  }

  return path
})

// 默认展开的子菜单
const defaultOpenedMenus = computed(() => {
  const { path } = route
  const opened = []

  // 如果当前在管理员设备管理页面，展开该菜单
  if (path.startsWith('/admin/devices/')) {
    opened.push('/admin/devices')
  }

  // 如果当前在设备管理页面（非列表页），展开该菜单
  if (path.startsWith('/devices/') && path !== '/devices/list') {
    opened.push('/devices')
  }

  return opened
})

// 构建菜单路由（扁平化）
const menuRoutes = computed(() => {
  const mainRoute = router.getRoutes().find(r => r.path === '/')
  const children = mainRoute?.children || []

  // 在菜单顶部添加可视化大屏（指向独立路由）
  const visualizationRoute = router.getRoutes().find(r => r.path === '/visualization')

  // 过滤出可作为父菜单的路由（有parent属性或被重定向的路由）
  const parentRoutes = children.filter(child =>
    child.meta?.parent ||
    (child.redirect && !child.meta?.hidden)
  )

  // 添加独立路由和普通路由（没有parent且没有redirect的）
  const normalRoutes = children.filter(child =>
    !child.meta?.parent &&
    !child.redirect &&
    !child.meta?.hidden
  )

  if (visualizationRoute) {
    return [visualizationRoute, ...parentRoutes, ...normalRoutes]
  }

  return [...parentRoutes, ...normalRoutes]
})

/**
 * 检查路由是否有子菜单
 */
const hasChildren = (route) => {
  if (!route.redirect) return false

  const mainRoute = router.getRoutes().find(r => r.path === '/')
  const children = mainRoute?.children || []

  // 查找以该路由路径为前缀的子路由
  return children.some(child =>
    child.path.startsWith(route.path + '/') &&
    child.meta?.parent === route.meta?.title
  )
}

/**
 * 获取子路由
 */
const getChildren = (parentPath) => {
  const mainRoute = router.getRoutes().find(r => r.path === '/')
  const children = mainRoute?.children || []

  const parentTitle = menuRoutes.value.find(r => r.path === parentPath)?.meta?.title

  return children.filter(child =>
    child.path.startsWith(parentPath + '/') &&
    child.meta?.parent === parentTitle
  )
}

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
  overflow-y: auto;
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
  min-height: calc(100% - 60px);
}

/* 滚动条样式 */
.app-sidebar::-webkit-scrollbar {
  width: 6px;
}

.app-sidebar::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.app-sidebar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(255, 255, 255, 0.3);
}
</style>
