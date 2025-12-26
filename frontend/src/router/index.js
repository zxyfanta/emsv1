import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  // 可视化大屏独立路由（不使用 MainLayout）
  {
    path: '/visualization',
    name: 'Visualization',
    component: () => import('@/views/visualization/VisualizationDashboard.vue'),
    meta: { title: '可视化大屏', requiresAuth: true, fullscreen: true }
  },
  {
    path: '/',
    component: () => import('@/components/Layout/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '数据概览', icon: 'DataAnalysis' }
      },
      // ========== 设备管理（所有用户）==========
      {
        path: 'devices',
        redirect: '/devices/list',
        meta: { title: '设备管理', icon: 'Monitor' }
      },
      {
        path: 'devices/list',
        name: 'DeviceList',
        component: () => import('@/views/devices/DeviceList.vue'),
        meta: { title: '设备列表', icon: 'Monitor', parent: '设备管理' }
      },
      {
        path: 'devices/activate',
        name: 'DeviceActivate',
        component: () => import('@/views/devices/DeviceActivate.vue'),
        meta: { title: '激活设备', icon: 'CircleCheck', parent: '设备管理' }
      },
      // ========== 管理员设备管理（仅ADMIN）==========
      {
        path: 'admin/devices',
        redirect: '/admin/devices/list',
        meta: { title: '设备管理', icon: 'Setting', roles: ['ADMIN'] }
      },
      {
        path: 'admin/devices/list',
        name: 'AdminDeviceManagement',
        component: () => import('@/views/admin/DeviceManagement.vue'),
        meta: { title: '所有设备', icon: 'List', roles: ['ADMIN'], parent: '管理员设备' }
      },
      {
        path: 'admin/devices/batch-import',
        name: 'BatchImportDevices',
        component: () => import('@/views/admin/BatchImportDevices.vue'),
        meta: { title: '批量导入', icon: 'Upload', roles: ['ADMIN'], parent: '管理员设备' }
      },
      {
        path: 'admin/devices/activation',
        name: 'DeviceActivationManagement',
        component: () => import('@/views/admin/DeviceActivationManagement.vue'),
        meta: { title: '激活码管理', icon: 'Key', roles: ['ADMIN'], parent: '管理员设备' }
      },
      {
        path: 'devices/create',
        name: 'DeviceCreate',
        component: () => import('@/views/devices/DeviceForm.vue'),
        meta: { title: '手动录入', hidden: true, roles: ['ADMIN'] }
      },
      {
        path: 'devices/:id/edit',
        name: 'DeviceEdit',
        component: () => import('@/views/devices/DeviceForm.vue'),
        meta: { title: '编辑设备', hidden: true }
      },
      // ========== 数据查询 ==========
      {
        path: 'radiation-data',
        name: 'RadiationData',
        component: () => import('@/views/data/RadiationData.vue'),
        meta: { title: '辐射数据', icon: 'TrendCharts' }
      },
      {
        path: 'environment-data',
        name: 'EnvironmentData',
        component: () => import('@/views/data/EnvironmentData.vue'),
        meta: { title: '环境数据', icon: 'Sunny' }
      },
      // ========== 系统管理（仅ADMIN）==========
      {
        path: 'companies',
        name: 'CompanyList',
        component: () => import('@/views/companies/CompanyList.vue'),
        meta: { title: '企业管理', icon: 'OfficeBuilding', roles: ['ADMIN'] }
      },
      {
        path: 'users',
        name: 'UserList',
        component: () => import('@/views/users/UserList.vue'),
        meta: { title: '用户管理', icon: 'User', roles: ['ADMIN'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth !== false) {
    if (!token) {
      next('/login')
    } else if (!userStore.userInfo) {
      userStore.fetchUserInfo().then(() => {
        next()
      }).catch(() => {
      localStorage.removeItem('token')
      next('/login')
      })
    } else {
      next()
    }
  } else {
    if (token && to.path === '/login') {
      next('/')
    } else {
      next()
    }
  }
})

export default router
