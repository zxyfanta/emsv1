<template>
  <div
    ref="dashboardRef"
    class="visualization-dashboard"
    :class="{ 'fullscreen': isFullscreen }"
  >
    <!-- 顶部标题栏 -->
    <div class="header-title">
      <Decoration8 :reverse="true" :color="['#00d4ff', '#0099ff']" style="width:300px; height:50px;" />
      <div class="title-wrapper">
        <div class="title-text">EMS设备可视化监控大屏</div>
        <div class="title-decoration">
          <Decoration10 :color="['#00d4ff', '#ff6b00']" style="width:200px; height:5px;" />
        </div>
      </div>
      <Decoration8 :color="['#00d4ff', '#0099ff']" style="width:300px; height:50px;" />
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
        <BorderBox8 :color="['#00d4ff', '#0099ff']" class="panel-container">
          <template #default>
            <div class="panel-header">
              <Decoration3 :color="['#00d4ff', '#ff6b00']" style="width:100%; height:15px;" />
              <span class="panel-title">设备统计</span>
            </div>
            <LeftPanel
              :devices="devices"
              :online-count="onlineCount"
              @device-click="handleDeviceClick"
            />
          </template>
        </BorderBox8>
      </div>

      <!-- 中间3D场景区 -->
      <div class="center-panel">
        <BorderBox11 title="3D场景可视化" :title-width="200" :color="['#00d4ff', '#0099ff']" class="scene-container">
          <div ref="canvasContainer" class="canvas-wrapper"></div>
          <!-- 场景内装饰 -->
          <div class="scene-decoration-top">
            <Decoration2 :color="['#00d4ff', '#ff6b00']" style="width:90%; height:5px; margin:0 auto;" />
          </div>
          <div class="scene-decoration-bottom">
            <Decoration10 :color="['#00d4ff', '#ff6b00']" style="width:80%; height:5px; margin:0 auto;" />
          </div>
          <!-- 场景角落装饰 -->
          <div class="corner-decoration top-left">
            <Decoration12 :color="['#00d4ff', '#ff6b00']" style="width:40px; height:40px;" />
          </div>
          <div class="corner-decoration top-right">
            <Decoration12 :color="['#00d4ff', '#ff6b00']" style="width:40px; height:40px;" />
          </div>
          <div class="corner-decoration bottom-left">
            <Decoration12 :color="['#00d4ff', '#ff6b00']" style="width:40px; height:40px;" />
          </div>
          <div class="corner-decoration bottom-right">
            <Decoration12 :color="['#00d4ff', '#ff6b00']" style="width:40px; height:40px;" />
          </div>
        </BorderBox11>
      </div>

      <!-- 右侧面板 -->
      <div class="right-panel">
        <BorderBox8 :reverse="true" :color="['#00d4ff', '#0099ff']" class="panel-container">
          <template #default>
            <div class="panel-header">
              <Decoration3 :color="['#00d4ff', '#ff6b00']" style="width:100%; height:15px;" />
              <span class="panel-title">设备列表</span>
            </div>
            <RightPanel
              :devices="devices"
              :selected-device="selectedDevice"
              @device-click="handleDeviceClick"
              @edit-device="handleEditDevice"
              @view-data="handleViewData"
            />
          </template>
        </BorderBox8>
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
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import screenfull from 'screenfull'
import { useAppStore } from '@/store/app'
// DataV组件已在main.js全局引入，无需按需引入
import DeviceDetailPanel from '@/components/visualization/DeviceDetailPanel.vue'
import LeftPanel from '@/components/visualization/LeftPanel.vue'
import RightPanel from '@/components/visualization/RightPanel.vue'
import { getAllDevices } from '@/api/device'
import { ElMessage } from 'element-plus'

const router = useRouter()
const appStore = useAppStore()
const dashboardRef = ref(null)
const canvasContainer = ref(null)
const devices = ref([])
const detailDrawer = ref(false)
const selectedDevice = ref(null)
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

// 切换全屏
const toggleFullscreen = () => {
  if (!screenfull.isEnabled) {
    ElMessage.warning('当前浏览器不支持全屏功能')
    return
  }
  screenfull.toggle()
}

// 监听全屏变化
const onFullscreenChange = () => {
  appStore.setFullscreen(screenfull.isFullscreen)
}

// 初始化Three.js场景
const initScene = () => {
  // 创建场景
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x0a0e1a)

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

onMounted(() => {
  initScene()
  loadDevices()
  renderer.domElement.addEventListener('click', onMouseClick)
  // 监听全屏变化事件
  if (screenfull.isEnabled) {
    screenfull.on('change', onFullscreenChange)
  }
})

onBeforeUnmount(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
  renderer.domElement.removeEventListener('click', onMouseClick)
  if (screenfull.isEnabled) {
    screenfull.off('change', onFullscreenChange)
  }
  cancelAnimationFrame(animationId)

  if (renderer) {
    renderer.dispose()
  }

  // 重置全屏状态
  appStore.setFullscreen(false)

  // 如果仍在全屏模式，退出全屏
  if (screenfull.isEnabled && screenfull.isFullscreen) {
    screenfull.exit()
  }
})
</script>

<style scoped>
.visualization-dashboard {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #0a0e1a 0%, #1a1f3a 100%);
  padding: 16px;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
}

/* 全屏模式样式 */
.visualization-dashboard.fullscreen {
  padding: 8px;
}

.visualization-dashboard.fullscreen .header-title {
  height: 50px;
}

.visualization-dashboard.fullscreen .title-text {
  font-size: 24px;
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
  background: linear-gradient(90deg, #00d4ff 25%, #0099ff);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  letter-spacing: 4px;
}

.title-decoration {
  opacity: 0.8;
}

/* 全屏按钮 */
.fullscreen-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: rgba(0, 212, 255, 0.1);
  border: 1px solid rgba(0, 212, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
  z-index: 100;
  color: #00d4ff;
}

.fullscreen-btn:hover {
  background: rgba(0, 212, 255, 0.2);
  border-color: rgba(0, 212, 255, 0.5);
  transform: scale(1.05);
}

.visualization-dashboard.fullscreen .fullscreen-btn {
  top: 8px;
  right: 8px;
}

.main-content {
  flex: 1;
  display: grid;
  grid-template-columns: 320px 1fr 320px;
  gap: 16px;
  min-height: 0;
}

/* 全屏模式下的布局调整 */
.visualization-dashboard.fullscreen .main-content {
  gap: 12px;
  grid-template-columns: 380px 1fr 380px;
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
  color: #42d392;
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
  opacity: 0.6;
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

/* 全屏模式下确保内容不被裁剪 */
.visualization-dashboard.fullscreen :deep(.dv-border-box-content) {
  overflow: hidden !important;
}
</style>
