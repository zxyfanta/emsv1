<template>
  <div class="app-sidebar">
    <div class="logo-container" v-if="!appStore.sidebarCollapsed">
      <h2>EMS系统</h2>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="appStore.sidebarCollapsed"
      :unique-opened="false"
      router
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
    >
      <!-- 遍历所有菜单分组 -->
      <template v-for="category in menuCategories" :key="category.key">
        <!-- 数据概览特殊处理（顶级独立项，不使用子菜单） -->
        <template v-if="category.key === '数据概览'">
          <template v-for="route in getCategoryRoutes(category.key)" :key="route.path">
            <el-menu-item
              v-if="!route.meta?.hidden"
              :index="route.path"
              :route="route.path"
            >
              <el-icon v-if="route.meta?.icon">
                <component :is="getIcon(route.meta.icon)" />
              </el-icon>
              <template #title>{{ route.meta?.title }}</template>
            </el-menu-item>
          </template>
        </template>

        <!-- 其他分组显示为子菜单 -->
        <el-sub-menu v-else :index="category.key">
          <template #title>
            <el-icon v-if="category.icon">
              <component :is="getIcon(category.icon)" />
            </el-icon>
            <span>{{ category.key }}</span>
          </template>

          <!-- 该分组下的所有路由 -->
          <template v-for="route in getCategoryRoutes(category.key)" :key="route.path">
            <el-menu-item
              v-if="!route.meta?.hidden"
              :index="route.path"
              :route="route.path"
            >
              <el-icon v-if="route.meta?.icon">
                <component :is="getIcon(route.meta.icon)" />
              </el-icon>
              <template #title>{{ route.meta?.title }}</template>
            </el-menu-item>
          </template>
        </el-sub-menu>
      </template>
    </el-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'
import { menuCategories } from '@/router/routes'
import * as ElementPlusIcons from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

// ========================================
// 当前激活的菜单（直接使用路径，无需复杂映射）
// ========================================
const activeMenu = computed(() => route.path)

// ========================================
// 获取所有已加载的路由
// ========================================
const loadedRoutes = computed(() => {
  const allRoutes = router.getRoutes()

  // 过滤出需要在菜单中显示的路由
  return allRoutes.filter(route => {
    // 不显示隐藏的路由
    if (route.meta?.hidden) return false

    // 不显示没有标题的路由
    if (!route.meta?.title) return false

    // 不显示根路径和布局组件
    if (route.path === '/' || route.path === '') return false

    // 不显示Login、403、404等错误页面
    if (route.name === 'Login' || route.name === 'Forbidden' || route.name === 'NotFound') {
      return false
    }

    return true
  })
})

// ========================================
// 根据分组获取路由
// ========================================
const getCategoryRoutes = (category) => {
  return loadedRoutes.value
    .filter(route => route.meta?.category === category)
    .sort((a, b) => (a.meta?.order || 0) - (b.meta?.order || 0))
}

// ========================================
// 获取图标组件
// ========================================
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
