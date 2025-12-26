import { createRouter, createWebHistory } from 'vue-router'
import {
  constantRoutes,
  publicRoutes,
  mainLayoutRoutes,
  userRoutes,
  adminRoutes,
  notFoundRoute
} from './routes'
import { setupPermissionGuard } from './permission'

/**
 * 创建路由实例
 * 初始时只加载常量路由、公共路由和主布局路由
 * 其他路由根据用户角色动态加载
 */
const router = createRouter({
  history: createWebHistory(),
  routes: [...constantRoutes, ...publicRoutes, ...mainLayoutRoutes]
})

/**
 * 标记路由是否已加载
 * 防止重复加载路由
 */
let routerLoaded = false

/**
 * 检查路由是否已加载
 * @returns {Boolean}
 */
export function isRouterLoaded() {
  return routerLoaded
}

/**
 * 动态添加路由
 * 根据用户角色加载相应的路由
 * @param {Object} userStore - 用户store
 */
export async function setupRouter(userStore) {
  // 避免重复加载
  if (routerLoaded) {
    return
  }

  try {
    // 将所有用户路由作为主布局的子路由添加
    userRoutes.forEach(route => {
      router.addRoute('MainLayout', route)
    })

    // 如果是管理员，添加管理员路由作为主布局的子路由
    if (userStore.isAdmin) {
      adminRoutes.forEach(route => {
        router.addRoute('MainLayout', route)
      })
    }

    // 最后添加404路由（必须放在所有其他路由之后）
    router.addRoute(notFoundRoute)

    // 标记路由已加载
    routerLoaded = true
  } catch (error) {
    console.error('❌ 动态路由加载失败:', error)
    throw error
  }
}

/**
 * 重置路由
 * 用于用户登出或切换账号时清除已加载的路由
 */
export function resetRouter() {
  routerLoaded = false
  // 创建新的路由实例
  const newRouter = createRouter({
    history: createWebHistory(),
    routes: [...constantRoutes, ...publicRoutes, ...mainLayoutRoutes]
  })
  // 替换当前路由 matcher
  router.matcher = newRouter.matcher
}

// 设置路由守卫
setupPermissionGuard(router)

export default router
