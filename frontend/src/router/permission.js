/**
 * 路由权限控制
 * 实现路由守卫和动态路由加载
 */
import { useUserStore } from '@/store/user'
import { useAppStore } from '@/store/app'
import { setupRouter, isRouterLoaded } from './index'

/**
 * 设置路由守卫
 * @param {Router} router - Vue Router实例
 */
export function setupPermissionGuard(router) {
  router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore()
    const appStore = useAppStore()
    const token = localStorage.getItem('token')

    // ========================================
    // 1. 公开页面（不需要认证）
    // ========================================
    if (to.meta.requiresAuth === false) {
      // 已登录用户访问登录页，重定向到首页
      if (token && to.path === '/login') {
        next('/')
        return
      }
      // 其他公开页面直接放行
      next()
      return
    }

    // ========================================
    // 2. 检查token是否存在
    // ========================================
    if (!token) {
      // 未登录，重定向到登录页
      next('/login')
      return
    }

    // ========================================
    // 3. 获取用户信息并加载动态路由
    // ========================================
    // 检查是否需要加载路由：userInfo不存在 或 路由未加载（页面刷新后routerLoaded被重置）
    if (!userStore.userInfo || !isRouterLoaded()) {
      try {
        // 如果用户信息不存在，先获取用户信息
        if (!userStore.userInfo) {
          await userStore.fetchUserInfo()
        }

        // 动态添加路由
        await setupRouter(userStore)

        // 动态路由加载后，重新导航到目标路由
        // 使用 replace: true 避免在浏览器历史记录中留下重复记录
        return next({ ...to, replace: true })
      } catch (error) {
        console.error('❌ 获取用户信息或加载路由失败:', error)
        // 失败，清除token并跳转到登录页
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        return next('/login')
      }
    }

    // ========================================
    // 4. 检查角色权限
    // ========================================
    if (to.meta.roles && to.meta.roles.length > 0) {
      const userRole = userStore.userRole

      // 检查用户角色是否在允许的角色列表中
      if (!to.meta.roles.includes(userRole)) {
        console.warn(`⚠️ 权限不足: 需要 ${to.meta.roles.join(' 或 ')} 角色，当前角色: ${userRole}`)

        // 跳转到403页面
        next('/403')
        return
      }
    }

    // ========================================
    // 5. 同步全屏状态
    // ========================================
    appStore.setFullscreen(to.meta.fullscreen || false)

    // ========================================
    // 6. 通过所有检查，放行
    // ========================================
    next()
  })

  // ========================================
  // 路由后置钩子（可选）
  // ========================================
  router.afterEach((to) => {
    // 设置页面标题
    if (to.meta.title) {
      document.title = `${to.meta.title} - EMS系统`
    } else {
      document.title = 'EMS系统'
    }
  })
}

/**
 * 检查路由是否可访问
 * @param {Object} route - 路由对象
 * @param {String} userRole - 用户角色
 * @returns {Boolean}
 */
export function hasRoutePermission(route, userRole) {
  // 如果没有指定角色要求，则所有用户都可访问
  if (!route.meta?.roles || route.meta.roles.length === 0) {
    return true
  }

  // 检查用户角色是否在允许的角色列表中
  return route.meta.roles.includes(userRole)
}

/**
 * 过滤可访问的路由
 * @param {Array} routes - 路由数组
 * @param {String} userRole - 用户角色
 * @returns {Array}
 */
export function filterAccessibleRoutes(routes, userRole) {
  return routes.filter(route => {
    // 隐藏的路由不显示在菜单中
    if (route.meta?.hidden) {
      return false
    }

    // 检查角色权限
    return hasRoutePermission(route, userRole)
  })
}
