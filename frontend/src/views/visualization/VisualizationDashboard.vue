<template>
  <div class="visualization-dashboard">
    <div ref="canvasContainer" class="canvas-container"></div>

    <!-- 设备详情抽屉 -->
    <el-drawer v-model="detailDrawer" title="设备详情" size="400px">
      <DeviceDetailPanel v-if="selectedDevice" :device="selectedDevice" />
    </el-drawer>

    <!-- 提示信息 -->
    <div class="info-panel">
      <el-card>
        <template #header>
          <span>设备统计</span>
        </template>
        <div class="stats">
          <div class="stat-item">
            <span class="label">总设备数：</span>
            <span class="value">{{ devices.length }}</span>
          </div>
          <div class="stat-item">
            <span class="label">已定位：</span>
            <span class="value">{{ devicesWithPosition.length }}</span>
          </div>
          <div class="stat-item">
            <span class="label">在线：</span>
            <span class="value online">{{ onlineCount }}</span>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import DeviceDetailPanel from '@/components/visualization/DeviceDetailPanel.vue'
import { getAllDevices } from '@/api/device'
import { ElMessage } from 'element-plus'

const canvasContainer = ref(null)
const devices = ref([])
const detailDrawer = ref(false)
const selectedDevice = ref(null)

let scene, camera, renderer, controls
let animationId
const deviceMarkers = []

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
  scene.background = new THREE.Color(0x1a1a2e)

  // 创建相机
  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight
  camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 1000)
  camera.position.set(0, 30, 40)
  camera.lookAt(0, 0, 0)

  // 创建渲染器
  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(width, height)
  canvasContainer.value.appendChild(renderer.domElement)

  // 创建控制器
  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05

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

  // 监听窗口大小变化
  window.addEventListener('resize', onWindowResize)

  // 开始动画循环
  animate()
}

// 窗口大小变化处理
const onWindowResize = () => {
  if (!canvasContainer.value) return
  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight

  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
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
    selectedDevice.value = device
    detailDrawer.value = true
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

onMounted(() => {
  initScene()
  loadDevices()
  renderer.domElement.addEventListener('click', onMouseClick)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onWindowResize)
  renderer.domElement.removeEventListener('click', onMouseClick)
  cancelAnimationFrame(animationId)

  if (renderer) {
    renderer.dispose()
  }
})
</script>

<style scoped>
.visualization-dashboard {
  width: 100%;
  height: 100vh;
  position: relative;
}

.canvas-container {
  width: 100%;
  height: 100%;
}

.info-panel {
  position: absolute;
  top: 20px;
  left: 20px;
  width: 200px;
  z-index: 10;
}

.stats {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
}

.stat-item .label {
  color: #606266;
}

.stat-item .value {
  font-weight: bold;
  color: #303133;
}

.stat-item .value.online {
  color: #67c23a;
}
</style>
