/**
 * 路由配置
 * 集中管理所有路由，按功能模块分组
 */

// ============================================
// 常量路由（所有用户可见，无需权限检查）
// ============================================
export const constantRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/Error/403.vue'),
    meta: {
      title: '无权限访问',
      requiresAuth: false,
      hidden: true
    }
  }
]

// ============================================
// 公共路由（所有已登录用户可访问）
// ============================================
export const publicRoutes = [
  {
    path: '/visualization',
    name: 'Visualization',
    component: () => import('@/views/visualization/VisualizationDashboard.vue'),
    meta: {
      title: '可视化大屏',
      icon: 'DataAnalysis',
      requiresAuth: true,
      fullscreen: true,
      category: '数据概览',
      order: 0
    }
  }
]

// ============================================
// 主布局路由（包含所有需要 MainLayout 的子路由）
// ============================================
export const mainLayoutRoutes = [
  {
    path: '/',
    name: 'MainLayout',
    component: () => import('@/components/Layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [] // 子路由将在 setupRouter 中动态添加
  }
]

// ============================================
// 普通用户路由
// ============================================
export const userRoutes = [
  // 数据概览
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: {
      title: '数据概览',
      icon: 'DataAnalysis',
      category: '数据概览',
      order: 1
    }
  },

  // 设备管理
  {
    path: '/devices/list',
    name: 'DeviceList',
    component: () => import('@/views/devices/DeviceList.vue'),
    meta: {
      title: '我的设备',
      icon: 'Monitor',
      category: '设备管理',
      order: 10
    }
  },
  {
    path: '/devices/activate',
    name: 'DeviceActivate',
    component: () => import('@/views/devices/DeviceActivate.vue'),
    meta: {
      title: '激活设备',
      icon: 'CircleCheck',
      category: '设备管理',
      order: 11
    }
  },

  // 数据查询
  {
    path: '/radiation-data',
    name: 'RadiationData',
    component: () => import('@/views/data/RadiationData.vue'),
    meta: {
      title: '辐射数据',
      icon: 'TrendCharts',
      category: '数据查询',
      order: 20
    }
  },
  {
    path: '/environment-data',
    name: 'EnvironmentData',
    component: () => import('@/views/data/EnvironmentData.vue'),
    meta: {
      title: '环境数据',
      icon: 'Sunny',
      category: '数据查询',
      order: 21
    }
  }
]

// ============================================
// 管理员路由
// ============================================
export const adminRoutes = [
  // 设备创建
  {
    path: '/devices/create',
    name: 'DeviceCreate',
    component: () => import('@/views/devices/DeviceForm.vue'),
    meta: {
      title: '手动录入',
      icon: 'Plus',
      category: '设备管理',
      order: 12,
      hidden: true  // 不在菜单中显示，只能通过设备列表进入
    }
  },
  {
    path: '/devices/:id/edit',
    name: 'DeviceEdit',
    component: () => import('@/views/devices/DeviceForm.vue'),
    meta: {
      title: '编辑设备',
      hidden: true
    }
  },

  // 管理员设备管理
  {
    path: '/admin/devices/list',
    name: 'AdminDeviceManagement',
    component: () => import('@/views/admin/DeviceManagement.vue'),
    meta: {
      title: '所有设备',
      icon: 'List',
      category: '设备管理',
      order: 13
    }
  },
  {
    path: '/admin/devices/batch-import',
    name: 'BatchImportDevices',
    component: () => import('@/views/admin/BatchImportDevices.vue'),
    meta: {
      title: '批量导入',
      icon: 'Upload',
      category: '设备管理',
      order: 14
    }
  },

  // 系统管理
  {
    path: '/companies',
    name: 'CompanyList',
    component: () => import('@/views/companies/CompanyList.vue'),
    meta: {
      title: '企业管理',
      icon: 'OfficeBuilding',
      category: '系统管理',
      order: 30
    }
  },
  {
    path: '/companies/create',
    name: 'CompanyCreate',
    component: () => import('@/views/companies/CompanyForm.vue'),
    meta: {
      title: '添加企业',
      hidden: true
    }
  },
  {
    path: '/companies/:id/edit',
    name: 'CompanyEdit',
    component: () => import('@/views/companies/CompanyForm.vue'),
    meta: {
      title: '编辑企业',
      hidden: true
    }
  },
  {
    path: '/users',
    name: 'UserList',
    component: () => import('@/views/users/UserList.vue'),
    meta: {
      title: '用户管理',
      icon: 'User',
      category: '系统管理',
      order: 31
    }
  },
  {
    path: '/users/create',
    name: 'UserCreate',
    component: () => import('@/views/users/UserForm.vue'),
    meta: {
      title: '添加用户',
      hidden: true
    }
  },
  {
    path: '/users/:id/edit',
    name: 'UserEdit',
    component: () => import('@/views/users/UserForm.vue'),
    meta: {
      title: '编辑用户',
      hidden: true
    }
  }
]

// ============================================
// 404路由（必须最后添加）
// ============================================
export const notFoundRoute = {
  path: '/:pathMatch(.*)*',
  name: 'NotFound',
  component: () => import('@/views/NotFound.vue'),
  meta: {
    title: '页面不存在',
    hidden: true
  }
}

// ============================================
// 路由分类配置（用于侧边栏菜单分组）
// ============================================
export const menuCategories = [
  {
    key: '数据概览',
    icon: 'DataAnalysis',
    order: 0
  },
  {
    key: '设备管理',
    icon: 'Monitor',
    order: 10
  },
  {
    key: '数据查询',
    icon: 'TrendCharts',
    order: 20
  },
  {
    key: '系统管理',
    icon: 'Setting',
    order: 30
  }
]
