<template>
  <div ref="rootContainer" class="root-container">
    <dv-full-screen-container>
      <div
        ref="dashboardRef"
        class="visualization-dashboard"
        :class="{ 'fullscreen': isFullscreen }"
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

      <!-- 右侧装饰 -->
      <dv-decoration-8 :color="['#1890ff', '#096dd9']" style="width:250px; height:40px;" />
    </div>

    <!-- 全屏按钮 -->
    <div class="fullscreen-btn" @click="toggleFullscreen">
      <el-icon :size="24">
        <component :is="isFullscreen ? 'Exit' : 'FullScreen'" />
      </el-icon>
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
              @device-click="handleDeviceClick"
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
            <div class="panel-header">
              <div class="panel-title">设备列表</div>
            </div>
            <RightPanel
              :devices="devices"
              :selected-device="selectedDevice"
              @device-click="handleDeviceClick"
              @edit-device="handleEditDevice"
              @view-data="handleViewData"
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
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { useAppStore } from '@/store/app'
// DataV组件已在main.js全局引入，无需按需引入
import DeviceDetailPanel from '@/components/visualization/DeviceDetailPanel.vue'
import LeftPanel from '@/components/visualization/LeftPanel.vue'
import RightPanel from '@/components/visualization/RightPanel.vue'
import { getAllDevices } from '@/api/device'
import { ElMessage } from 'element-plus'

const router = useRouter()
const appStore = useAppStore()
const rootContainer = ref(null) // 根容器，用于全屏
const dashboardRef = ref(null)
const canvasContainer = ref(null)
const devices = ref([])
const detailDrawer = ref(false)
const selectedDevice = ref(null)

// 使用 appStore 管理全屏状态
const isFullscreen = computed(() => appStore.isFullscreen)

let scene, camera, renderer, controls
let animationId
const deviceMarkers = []
let resizeObserver = null

// 过滤出已设置位置的设备
const devicesWithPosition = computed(() => {
  return devices.value.filter(d => d.positionX !== null && d.positionY !== null)
})

// 在线设备数量
const onlineCount = computed(() => {
  return devices.value.filter(d => d.status === 'ONLINE').length
})

// 切换全屏（使用 appStore 管理专注模式）
const toggleFullscreen = () => {
  appStore.toggleFullscreen()

  // 触发窗口resize事件，确保Three.js场景正确调整
  setTimeout(() => {
    window.dispatchEvent(new Event('resize'))
  }, 100)
}

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
  camera.position.set(0, 35, 50)
  camera.lookAt(0, 0, 0)

  // 创建渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(width, height)
  canvasContainer.value.appendChild(renderer.domElement)

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
  scene.add(directionalLight)

  // 添加地面
  const groundGeometry = new THREE.PlaneGeometry(50, 50)
  const groundMaterial = new THREE.MeshBasicMaterial({ color: 0x333333 })
  const ground = new THREE.Mesh(groundGeometry, groundMaterial)
  ground.rotation.x = -Math.PI / 2
  scene.add(ground)

  // 添加中心模型
  const centerGeometry = new THREE.BoxGeometry(10, 10, 10)
  const centerMaterial = new THREE.MeshStandardMaterial({
    color: 0x4a90e2,
    transparent: true,
    opacity: 0.8
  })
  const centerCube = new THREE.Mesh(centerGeometry, centerMaterial)
  centerCube.position.set(0, 5, 0)
  scene.add(centerCube)

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
  }
}

// 创建设备标记
const createDeviceMarkers = () => {
  // 清除旧标记
  deviceMarkers.forEach(marker => scene.remove(marker))
  deviceMarkers.length = 0

  // 创建新标记
  devicesWithPosition.value.forEach(device => {
    const x = ((device.positionX || 50) - 50) * 0.4
    const z = ((device.positionY || 50) - 50) * 0.4

    // 创建球体
    const geometry = new THREE.SphereGeometry(0.5, 32, 32)
    const isOnline = device.status === 'ONLINE'
    const isRadiation = device.deviceType === 'RADIATION_MONITOR'

    let color
    if (!isOnline) {
      color = isRadiation ? 0x8b0000 : 0x2e4d28
    } else {
      color = isRadiation ? 0xff4444 : 0x44cc44
    }

    const material = new THREE.MeshStandardMaterial({ color })
    const sphere = new THREE.Mesh(geometry, material)
    sphere.position.set(x, 0.5, z)
    sphere.userData = { device }
    scene.add(sphere)
    deviceMarkers.push(sphere)
  })
}

// 动画循环
const animate = () => {
  animationId = requestAnimationFrame(animate)
  controls.update()
  renderer.render(scene, camera)
}

// 点击处理
const onMouseClick = (event) => {
  if (!canvasContainer.value) return

  const rect = renderer.domElement.getBoundingClientRect()
  const mouse = new THREE.Vector2(
    ((event.clientX - rect.left) / rect.width) * 2 - 1,
    -((event.clientY - rect.top) / rect.height) * 2 + 1
  )

  const raycaster = new THREE.Raycaster()
  raycaster.setFromCamera(mouse, camera)

  const intersects = raycaster.intersectObjects(deviceMarkers)
  if (intersects.length > 0) {
    const device = intersects[0].object.userData.device
    handleDeviceClick(device)
  }
}

// 加载设备数据
const loadDevices = async () => {
  try {
    const res = await getAllDevices()
    if (res.status === 200) {
      devices.value = res.data.content || []
      createDeviceMarkers()
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

onMounted(async () => {
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

  if (renderer) {
    renderer.dispose()
  }

  // 退出专注模式
  if (appStore.isFullscreen) {
    appStore.setFullscreen(false)
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

/* 全屏按钮 */
.fullscreen-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 40px;
  height: 40px;
  border-radius: 6px;
  background: rgba(24, 144, 255, 0.1);
  border: 1px solid rgba(24, 144, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  z-index: 100;
  color: #1890ff;
}

.fullscreen-btn:hover {
  background: rgba(24, 144, 255, 0.2);
  border-color: rgba(24, 144, 255, 0.5);
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
