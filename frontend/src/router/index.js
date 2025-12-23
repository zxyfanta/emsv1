import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
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
      {
        path: 'visualization',
        name: 'Visualization',
        component: () => import('@/views/visualization/VisualizationDashboard.vue'),
        meta: { title: '可视化大屏', icon: 'DataBoard' }
      },
      {
        path: 'devices',
        name: 'DeviceList',
        component: () => import('@/views/devices/DeviceList.vue'),
        meta: { title: '设备管理', icon: 'Monitor' }
      },
      {
        path: 'devices/create',
        name: 'DeviceCreate',
        component: () => import('@/views/devices/DeviceForm.vue'),
        meta: { title: '添加设备', hidden: true }
      },
      {
        path: 'devices/:id/edit',
        name: 'DeviceEdit',
        component: () => import('@/views/devices/DeviceForm.vue'),
        meta: { title: '编辑设备', hidden: true }
      },
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
