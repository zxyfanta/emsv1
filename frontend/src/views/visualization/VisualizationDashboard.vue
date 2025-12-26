<template>
  <div class="root-container">
    <dv-full-screen-container>
      <div
        ref="dashboardRef"
        class="visualization-dashboard"
        :class="{ 'standalone': isStandalone }"
      >
    <!-- 顶部标题栏 -->
    <div class="header-title">
      <!-- 左侧装饰 -->
      <dv-decoration-8 :reverse="true" :color="['#1890ff', '#096dd9']" style="width:250px; height:40px;" />

      <!-- 标题边框 -->
      <dv-border-box-13 class="title-border-box" :color="['#1890ff', '#096dd9']">
        <div class="title-wrapper">
          <div class="title-text">EMS设备可视化监控大屏</div>
          <!-- 标题下方装饰线 -->
          <dv-decoration-10 :color="['#1890ff']" style="width:180px; height:3px; margin-top:4px;" />
        </div>
      </dv-border-box-13>

      <!-- 企业选择器（仅管理员可见）- 固定悬浮 -->
      <div v-if="isAdmin" class="company-selector-fixed">
        <el-select
          v-model="selectedCompanyId"
          placeholder="选择企业"
          @change="handleCompanyChange"
          size="small"
          popper-class="company-select-dropdown"
        >
          <el-option
            v-for="company in companies"
            :key="company.id"
            :label="company.companyName"
            :value="company.id"
          />
        </el-select>
      </div>

      <!-- 右侧装饰 -->
      <dv-decoration-8 :color="['#1890ff', '#096dd9']" style="width:250px; height:40px;" />
    </div>

    <!-- 三列布局 -->
    <div class="main-content">
      <!-- 左侧面板 -->
      <div class="left-panel">
        <dv-border-box-8 class="panel-container" :color="['#1890ff', '#096dd9']">
          <template #default>
            <div class="panel-header">
              <div class="panel-title">设备统计</div>
            </div>
            <LeftPanel
              :devices="devices"
              :online-count="onlineCount"
              @back="goBack"
            />
          </template>
        </dv-border-box-8>
      </div>

      <!-- 中间3D场景区 -->
      <div class="center-panel">
        <dv-border-box-11 title="3D场景可视化" :title-width="200" :color="['#1890ff', '#096dd9']" class="scene-container">
          <div ref="canvasContainer" class="canvas-wrapper"></div>

          <!-- 场景内装饰 -->
          <div class="scene-decoration-top">
            <dv-decoration-2 :color="['#1890ff']" style="width:90%; height:3px; margin:0 auto;" />
          </div>
          <div class="scene-decoration-bottom">
            <dv-decoration-10 :color="['#1890ff']" style="width:80%; height:3px; margin:0 auto;" />
          </div>

          <!-- 场景角落装饰 -->
          <div class="corner-decoration top-left">
            <dv-decoration-12 :color="['#1890ff']" style="width:35px; height:35px;" />
          </div>
          <div class="corner-decoration top-right">
            <dv-decoration-12 :color="['#1890ff']" style="width:35px; height:35px;" />
          </div>
          <div class="corner-decoration bottom-left">
            <dv-decoration-12 :color="['#1890ff']" style="width:35px; height:35px;" />
          </div>
          <div class="corner-decoration bottom-right">
            <dv-decoration-12 :color="['#1890ff']" style="width:35px; height:35px;" />
          </div>
        </dv-border-box-11>
      </div>

      <!-- 右侧面板 -->
      <div class="right-panel">
        <dv-border-box-8 :reverse="true" class="panel-container" :color="['#1890ff', '#096dd9']">
          <template #default>
            <RightPanel
              :devices="devices"
            />
          </template>
        </dv-border-box-8>
      </div>
    </div>

    <!-- 设备详情抽屉（保持z-index管理） -->
    <el-drawer
      v-model="detailDrawer"
      title="设备详情"
      size="400px"
      :z-index="9999"
    >
      <DeviceDetailPanel v-if="selectedDevice" :device="selectedDevice" />
    </el-drawer>
  </div>
  </dv-full-screen-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { CSS2DRenderer } from 'three/examples/jsm/renderers/CSS2DRenderer.js'
import { createRegionOutline, regionOutlineData } from '@/constants/regionOutline'
import { createAllDeviceModels, animateDevices, handleDeviceClick as handleDevice3DClick, highlightDevice, initCSS2DRenderer } from '@/utils/device3DModels'
import DeviceDetailPanel from '@/components/visualization/DeviceDetailPanel.vue'
import LeftPanel from '@/components/visualization/LeftPanel.vue'
import RightPanel from '@/components/visualization/RightPanel.vue'
import { getAllDevices } from '@/api/device'
import { getCompanyList } from '@/api/company'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const dashboardRef = ref(null)
const canvasContainer = ref(null)
const devices = ref([])
const detailDrawer = ref(false)
const selectedDevice = ref(null)

// 企业相关状态（管理员可用）
const companies = ref([])
const selectedCompanyId = ref(null)

// 检查是否为管理员
const isAdmin = computed(() => {
  return userStore.userRole === 'ADMIN'
})

// 检查是否为独立路由模式（不使用 MainLayout）
const isStandalone = computed(() => {
  return router.currentRoute.value.meta?.fullscreen === true
})

let scene, camera, renderer, css2DRenderer, controls
let animationId
const deviceMarkers = []
let devices3DGroup = null
let resizeObserver = null
let regionOutline = null

// 过滤出已设置位置的设备
const devicesWithPosition = computed(() => {
  return devices.value.filter(d => d.positionX !== null && d.positionY !== null)
})

// 在线设备数量
const onlineCount = computed(() => {
  return devices.value.filter(d => d.status === 'ONLINE').length
})

// 初始化Three.js场景
const initScene = () => {
  // 创建场景
  scene = new THREE.Scene()
  // 使用渐变顶部的深蓝色，保持视觉统一
  scene.background = new THREE.Color(0x0a192f)

  // 创建相机
  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight
  camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 1000)
  camera.position.set(0, 40, 60)
  camera.lookAt(0, 0, 0)

  // 创建WebGL渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(window.devicePixelRatio)
  canvasContainer.value.appendChild(renderer.domElement)

  // 创建CSS2D渲染器（用于HTML标签）
  css2DRenderer = initCSS2DRenderer(canvasContainer.value, renderer)

  // 创建控制器
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.minDistance = 20
  controls.maxDistance = 100

  // 添加光源
  const ambientLight = new THREE.AmbientLight(0xffffff, 0.5)
  scene.add(ambientLight)

  const directionalLight = new THREE.DirectionalLight(0xffffff, 1)
  directionalLight.position.set(10, 20, 10)
  directionalLight.castShadow = true
  scene.add(directionalLight)

  // 添加点光源（增强设备高光效果）
  const pointLight1 = new THREE.PointLight(0x1890ff, 0.5, 50)
  pointLight1.position.set(20, 30, 20)
  scene.add(pointLight1)

  const pointLight2 = new THREE.PointLight(0x1890ff, 0.5, 50)
  pointLight2.position.set(-20, 30, -20)
  scene.add(pointLight2)

  // 添加网格地面（辅助网格）
  const gridHelper = new THREE.GridHelper(100, 50, 0x1890ff, 0x1a2332)
  scene.add(gridHelper)

  // 创建地区轮廓
  regionOutline = createRegionOutline(scene, regionOutlineData)

  // 创建3D设备模型组
  devices3DGroup = createAllDeviceModels(devicesWithPosition.value, THREE, {
    scale: 1,
    showLabel: true,
    animated: true
  })
  scene.add(devices3DGroup)

  // 使用ResizeObserver监听容器尺寸变化
  resizeObserver = new ResizeObserver(onCanvasResize)
  resizeObserver.observe(canvasContainer.value)

  // 开始动画循环
  animate()
}

// Canvas容器resize处理
const onCanvasResize = (entries) => {
  for (let entry of entries) {
    const { width, height } = entry.contentRect
    camera.aspect = width / height
    camera.updateProjectionMatrix()
    renderer.setSize(width, height)
    if (css2DRenderer) {
      css2DRenderer.setSize(width, height)
    }
  }
}

// 更新3D设备模型
const updateDevice3DModels = () => {
  if (!devices3DGroup) return

  // 移除旧的设备模型
  scene.remove(devices3DGroup)

  // 创建新的设备模型
  devices3DGroup = createAllDeviceModels(devicesWithPosition.value, THREE, {
    scale: 1,
    showLabel: true,
    animated: true
  })
  scene.add(devices3DGroup)
}

// 动画循环
const animate = () => {
  animationId = requestAnimationFrame(animate)

  const time = Date.now() * 0.001

  // 更新设备动画（呼吸灯效果）
  if (devices3DGroup) {
    animateDevices(devices3DGroup, time)
  }

  controls.update()
  renderer.render(scene, camera)

  // 渲染CSS2D标签
  if (css2DRenderer) {
    css2DRenderer.render(scene, camera)
  }
}

// 点击处理
const onMouseClick = (event) => {
  if (!canvasContainer.value || !devices3DGroup) return

  handleDevice3DClick(event, camera, devices3DGroup, (device) => {
    handleDeviceClick(device)
  })
}

// 加载企业列表（仅管理员）
const loadCompanies = async () => {
  // 调试日志：检查用户角色
  console.log('[企业选择器调试] 当前用户角色:', userStore.userRole)
  console.log('[企业选择器调试] isAdmin值:', isAdmin.value)
  console.log('[企业选择器调试] userInfo:', userStore.userInfo)

  if (!isAdmin.value) {
    console.warn('[企业选择器调试] 非管理员用户，跳过企业列表加载')
    return
  }

  try {
    console.log('[企业选择器调试] 开始加载企业列表...')
    const res = await getCompanyList({ size: 1000 })
    console.log('[企业选择器调试] 企业列表响应:', res)

    if (res.status === 200) {
      companies.value = res.data.content || []
      console.log('[企业选择器调试] 解析后的企业列表:', companies.value)

      // 默认选择第一个企业
      if (companies.value.length > 0 && !selectedCompanyId.value) {
        selectedCompanyId.value = companies.value[0].id
        console.log('[企业选择器调试] 默认选择企业ID:', selectedCompanyId.value)
      }
    }
  } catch (error) {
    console.error('[企业选择器调试] 加载企业列表失败:', error)
  }
}

// 加载设备数据
const loadDevices = async () => {
  try {
    // 管理员：根据选择的企业过滤设备
    // 普通用户：加载自己企业的设备
    const params = { size: 1000 }
    if (isAdmin.value && selectedCompanyId.value) {
      params.companyId = selectedCompanyId.value
    }

    const res = await getAllDevices(params)
    if (res.status === 200) {
      devices.value = res.data.content || []
      // 更新3D设备模型
      updateDevice3DModels()
    }
  } catch (error) {
    ElMessage.error('加载设备数据失败')
  }
}

// 处理设备点击
const handleDeviceClick = (device) => {
  selectedDevice.value = device
  detailDrawer.value = true
}

// 处理编辑设备
const handleEditDevice = (device) => {
  router.push(`/devices/${device.id}/edit`)
}

// 处理查看数据
const handleViewData = (device) => {
  if (device.deviceType === 'RADIATION_MONITOR') {
    router.push('/radiation-data')
  } else {
    router.push('/environment-data')
  }
}

// 返回系统（独立路由模式下使用）
const goBack = () => {
  router.push('/dashboard')
}

// 处理企业切换
const handleCompanyChange = (companyId) => {
  selectedCompanyId.value = companyId
  loadDevices()
}

// 监听企业切换
watch(selectedCompanyId, () => {
  if (selectedCompanyId.value) {
    loadDevices()
  }
})

onMounted(async () => {
  // 先加载企业列表（如果是管理员）
  await loadCompanies()

  // 立即加载设备数据，不依赖 canvasContainer 的就绪状态
  loadDevices()

  // 等待 DOM 完全渲染（dv-full-screen-container 需要更多时间初始化）
  await nextTick()
  await nextTick() // 双重 nextTick 确保 DataV 容器初始化完成

  // 3D 场景初始化需要等待容器就绪
  const initSceneWhenReady = () => {
    if (!canvasContainer.value) {
      console.warn('[initScene] canvasContainer 未就绪，等待中...')
      setTimeout(initSceneWhenReady, 200)
      return
    }

    const { clientWidth, clientHeight } = canvasContainer.value
    if (clientWidth === 0 || clientHeight === 0) {
      console.warn('[initScene] 容器尺寸为 0，等待容器初始化...')
      setTimeout(initSceneWhenReady, 200)
      return
    }

    // 容器就绪，初始化场景
    initScene()
    renderer.domElement.addEventListener('click', onMouseClick)
  }

  initSceneWhenReady()
})

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
  renderer.domElement.removeEventListener('click', onMouseClick)
  cancelAnimationFrame(animationId)

  // 清理CSS2D渲染器
  if (css2DRenderer && css2DRenderer.domElement && css2DRenderer.domElement.parentNode) {
    css2DRenderer.domElement.parentNode.removeChild(css2DRenderer.domElement)
  }

  // 清理3D设备模型
  if (devices3DGroup) {
    devices3DGroup.children.forEach(deviceGroup => {
      deviceGroup.children.forEach(child => {
        if (child.geometry) child.geometry.dispose()
        if (child.material) {
          if (Array.isArray(child.material)) {
            child.material.forEach(m => m.dispose())
          } else {
            child.material.dispose()
          }
        }
      })
    })
    scene.remove(devices3DGroup)
  }

  // 清理地区轮廓
  if (regionOutline) {
    regionOutline.children.forEach(child => {
      if (child.geometry) child.geometry.dispose()
      if (child.material) {
        if (Array.isArray(child.material)) {
          child.material.forEach(m => m.dispose())
        } else {
          child.material.dispose()
        }
      }
    })
    scene.remove(regionOutline)
  }

  if (renderer) {
    renderer.dispose()
  }
})
</script>

<style scoped>
.root-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.visualization-dashboard {
  width: 100%;
  height: 100%; /* 改为 100%，适配 DataV 全屏容器 */
  display: flex;
  flex-direction: column;
  /* 四段渐变深蓝背景：顶部科技蓝 → 中上高亮 → 中下科技蓝 → 底部深蓝（亮度提升） */
  background: linear-gradient(180deg, #0d47a1 0%, #1565c0 40%, #0d47a1 70%, #072743 100%);
  padding: 16px;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
}

.header-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  height: 60px;
  flex-shrink: 0;
  position: relative;
}

.title-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.title-text {
  font-size: 28px;
  font-weight: bold;
  color: #e8e8e8;
  letter-spacing: 4px;
}

/* 固定悬浮企业选择器 */
.company-selector-fixed {
  position: fixed;
  top: 20px;
  right: 30px;
  z-index: 1000;
  background: rgba(10, 25, 47, 0.85);
  border: 1px solid rgba(24, 144, 255, 0.4);
  border-radius: 8px;
  padding: 8px 12px;
  backdrop-filter: blur(10px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.2);
  transition: all 0.3s;
}

.company-selector-fixed:hover {
  background: rgba(10, 25, 47, 0.95);
  border-color: rgba(24, 144, 255, 0.6);
  box-shadow: 0 6px 16px rgba(24, 144, 255, 0.3);
}

.company-selector-fixed :deep(.el-select__wrapper) {
  background-color: rgba(24, 144, 255, 0.1);
  border: 1px solid rgba(24, 144, 255, 0.3);
  border-radius: 4px;
  box-shadow: none;
}

.company-selector-fixed :deep(.el-input__inner) {
  background-color: transparent;
  color: #e8e8e8;
  border: none;
  font-size: 14px;
}

.company-selector-fixed :deep(.el-select__caret) {
  color: #1890ff;
}

/* 下拉面板样式 */
.company-select-dropdown {
  background: rgba(10, 25, 47, 0.95) !important;
  border: 1px solid rgba(24, 144, 255, 0.4) !important;
}

.company-select-dropdown :deep(.el-select-dropdown__item) {
  color: #e8e8e8;
  background: transparent;
}

.company-select-dropdown :deep(.el-select-dropdown__item:hover) {
  background: rgba(24, 144, 255, 0.2);
  color: #1890ff;
}

.company-select-dropdown :deep(.el-select-dropdown__item.is-selected) {
  background: rgba(24, 144, 255, 0.3);
  color: #1890ff;
  font-weight: bold;
}

.title-decoration {
  opacity: 0.8;
}

/* 顶部标题边框样式 */
.title-border-box {
  padding: 8px 30px;
}

:deep(.title-border-box .dv-border-box-content) {
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.main-content {
  flex: 1;
  display: grid;
  grid-template-columns: 320px 1fr 320px;
  gap: 16px;
  min-height: 0;
}

.left-panel,
.right-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.panel-container {
  flex: 1;
  width: 100%;
  height: 100%;
  position: relative;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 10;
}

.panel-title {
  font-size: 14px;
  font-weight: bold;
  color: #e8e8e8;
  letter-spacing: 2px;
}

.center-panel {
  position: relative;
  min-height: 0;
}

.scene-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.canvas-wrapper {
  width: 100%;
  height: 100%;
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
}

/* 场景装饰层 */
.scene-decoration-top {
  position: absolute;
  top: 40px;
  left: 0;
  right: 0;
  z-index: 5;
  pointer-events: none;
}

.scene-decoration-bottom {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  z-index: 5;
  pointer-events: none;
}

.corner-decoration {
  position: absolute;
  z-index: 5;
  pointer-events: none;
  opacity: 0.7;
}

.corner-decoration.top-left {
  top: 50px;
  left: 20px;
}

.corner-decoration.top-right {
  top: 50px;
  right: 20px;
}

.corner-decoration.bottom-left {
  bottom: 30px;
  left: 20px;
}

.corner-decoration.bottom-right {
  bottom: 30px;
  right: 20px;
}

/* DataV边框内部容器样式调整 */
:deep(.dv-border-box-content) {
  display: flex !important;
  flex-direction: column !important;
  height: 100% !important;
  position: relative !important;
}

</style>
