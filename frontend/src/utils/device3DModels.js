/**
 * 3D设备模型创建工具
 *
 * 功能：
 * - 创建美观的3D设备模型
 * - 支持辐射监测设备和环境监测设备
 * - 带有设备标签（ID和名称）
 * - 状态指示（在线/离线）
 */

import * as ThreeJS from 'three'
import { CSS2DRenderer as CSS2DRendererClass, CSS2DObject } from 'three/examples/jsm/renderers/CSS2DRenderer.js'

/**
 * 设备类型配置
 */
export const DEVICE_TYPES = {
  RADIATION_MONITOR: {
    name: '辐射监测设备',
    color: 0xff4444,      // 红色
    onlineColor: 0xff6666,
    offlineColor: 0x8b0000,
    iconColor: '#ff4444'
  },
  ENVIRONMENT_MONITOR: {
    name: '环境监测设备',
    color: 0x44cc44,      // 绿色
    onlineColor: 0x66ff66,
    offlineColor:0x2e4d28,
    iconColor: '#44cc44'
  }
}

/**
 * 创建设备3D模型
 * @param {Object} device - 设备数据对象
 * @param {string} device.id - 设备ID
 * @param {string} device.name - 设备名称
 * @param {string} device.deviceType - 设备类型 (RADIATION_MONITOR/ENVIRONMENT_MONITOR)
 * @param {string} device.status - 设备状态 (ONLINE/OFFLINE)
 * @param {number} device.positionX - X坐标
 * @param {number} device.positionY - Y坐标（对应3D中的Z坐标）
 * @param {Object} THREE - Three.js命名空间
 * @param {Object} options - 配置选项
 * @returns {THREE.Group} - 设备3D对象组
 */
export function createDevice3DModel(device, THREE, options = {}) {
  const {
    scale = 1,
    showLabel = true,
    animated = true
  } = options

  const group = new THREE.Group()
  const typeConfig = DEVICE_TYPES[device.deviceType] || DEVICE_TYPES.ENVIRONMENT_MONITOR
  const isOnline = device.status === 'ONLINE'

  // 颜色配置
  const primaryColor = isOnline ? typeConfig.onlineColor : typeConfig.offlineColor
  const emissiveColor = isOnline ? primaryColor : 0x000000
  const emissiveIntensity = isOnline ? 0.3 : 0

  // 计算位置（将 positionX, positionY 转换为 3D 坐标）
  const x = ((device.positionX || 50) - 50) * 0.4
  const z = ((device.positionY || 50) - 50) * 0.4
  const y = 0  // 固定高度

  group.position.set(x, y, z)

  // ========== 1. 底座（圆形平台）==========
  const baseGeometry = new THREE.CylinderGeometry(1.5, 1.8, 0.3, 32)
  const baseMaterial = new THREE.MeshStandardMaterial({
    color: 0x333333,
    metalness: 0.8,
    roughness: 0.2
  })
  const base = new THREE.Mesh(baseGeometry, baseMaterial)
  base.position.y = 0.15
  base.castShadow = true
  base.receiveShadow = true
  group.add(base)

  // 底座发光环（在线时发光）
  if (isOnline) {
    const ringGeometry = new THREE.RingGeometry(1.3, 1.5, 32)
    const ringMaterial = new THREE.MeshBasicMaterial({
      color: primaryColor,
      transparent: true,
      opacity: 0.6,
      side: THREE.DoubleSide
    })
    const ring = new THREE.Mesh(ringGeometry, ringMaterial)
    ring.rotation.x = -Math.PI / 2
    ring.position.y = 0.31
    group.add(ring)
  }

  // ========== 2. 设备主体（圆柱+圆锥）==========

  // 主体圆柱
  const bodyGeometry = new THREE.CylinderGeometry(0.8, 1, 2, 32)
  const bodyMaterial = new THREE.MeshStandardMaterial({
    color: 0xe8e8e8,
    metalness: 0.3,
    roughness: 0.7
  })
  const body = new THREE.Mesh(bodyGeometry, bodyMaterial)
  body.position.y = 1.3
  body.castShadow = true
  group.add(body)

  // 设备类型指示条（环绕主体）
  const stripeGeometry = new THREE.CylinderGeometry(0.85, 0.85, 0.5, 32)
  const stripeMaterial = new THREE.MeshStandardMaterial({
    color: primaryColor,
    emissive: emissiveColor,
    emissiveIntensity: emissiveIntensity,
    metalness: 0.5,
    roughness: 0.3
  })
  const stripe = new THREE.Mesh(stripeGeometry, stripeMaterial)
  stripe.position.y = 1.5
  group.add(stripe)

  // ========== 3. 顶部传感器/天线 ==========

  if (device.deviceType === 'RADIATION_MONITOR') {
    // 辐射设备：Geiger计数器形状
    const sensorGeometry = new THREE.ConeGeometry(0.5, 1.5, 32)
    const sensorMaterial = new THREE.MeshStandardMaterial({
      color: primaryColor,
      emissive: emissiveColor,
      emissiveIntensity: emissiveIntensity,
      metalness: 0.4,
      roughness: 0.4
    })
    const sensor = new THREE.Mesh(sensorGeometry, sensorMaterial)
    sensor.position.y = 3
    sensor.castShadow = true
    group.add(sensor)

    // 天线
    const antennaGeometry = new THREE.CylinderGeometry(0.05, 0.05, 0.8, 8)
    const antennaMaterial = new THREE.MeshStandardMaterial({
      color: 0x666666,
      metalness: 0.9,
      roughness: 0.1
    })
    const antenna = new THREE.Mesh(antennaGeometry, antennaMaterial)
    antenna.position.y = 4
    group.add(antenna)

  } else {
    // 环境设备：多个传感器探头
    const sensorPositions = [
      { x: 0, z: 0 },
      { x: 0.5, z: 0 },
      { x: -0.5, z: 0 },
      { x: 0, z: 0.5 },
      { x: 0, z: -0.5 }
    ]

    sensorPositions.forEach(pos => {
      const sensorGeometry = new THREE.SphereGeometry(0.2, 16, 16)
      const sensorMaterial = new THREE.MeshStandardMaterial({
        color: primaryColor,
        emissive: emissiveColor,
        emissiveIntensity: emissiveIntensity,
        metalness: 0.6,
        roughness: 0.3
      })
      const sensor = new THREE.Mesh(sensorGeometry, sensorMaterial)
      sensor.position.set(pos.x, 2.8, pos.z)
      group.add(sensor)
    })
  }

  // ========== 4. 状态指示灯 ==========
  const lightGeometry = new THREE.SphereGeometry(0.15, 16, 16)
  const lightMaterial = new THREE.MeshBasicMaterial({
    color: isOnline ? 0x00ff00 : 0xff0000
  })
  const statusLight = new THREE.Mesh(lightGeometry, lightMaterial)
  statusLight.position.set(1.2, 1.5, 0)
  group.add(statusLight)

  // 添加点光源（在线时）
  if (isOnline) {
    const pointLight = new THREE.PointLight(primaryColor, 0.5, 5)
    pointLight.position.set(1.2, 1.5, 0)
    group.add(pointLight)
  }

  // ========== 5. 设备标签（HTML）==========
  if (showLabel) {
    const labelDiv = document.createElement('div')
    labelDiv.className = 'device-label'
    labelDiv.innerHTML = `
      <div class="device-label-content" style="
        background: rgba(24, 144, 255, 0.9);
        border: 1px solid ${typeConfig.iconColor};
        border-radius: 4px;
        padding: 4px 8px;
        color: white;
        font-size: 12px;
        font-weight: bold;
        white-space: nowrap;
        text-shadow: 0 1px 2px rgba(0,0,0,0.3);
        box-shadow: 0 2px 8px rgba(0,0,0,0.3);
      ">
        <div style="font-size: 10px; opacity: 0.8;">${device.deviceCode || device.id}</div>
        <div style="font-size: 11px; margin-top: 2px;">${device.name}</div>
        <div style="
          font-size: 9px;
          margin-top: 2px;
          padding: 2px 4px;
          border-radius: 2px;
          background: ${isOnline ? 'rgba(82, 196, 26, 0.8)' : 'rgba(255, 77, 79, 0.8)'};
        ">${isOnline ? '在线' : '离线'}</div>
      </div>
    `

    const label = new CSS2DObject(labelDiv)
    label.position.set(0, 4.5, 0)
    group.add(label)
  }

  // 存储设备数据，便于后续交互
  group.userData = {
    device: device,
    isDevice: true
  }

  // 添加动画（如果启用）
  if (animated && isOnline) {
    group.userData.animated = true
    group.userData.animationPhase = Math.random() * Math.PI * 2
  }

  return group
}

/**
 * 创建所有设备的3D模型
 * @param {Array} devices - 设备数组
 * @param {Object} THREE - Three.js命名空间
 * @param {Object} options - 配置选项
 * @returns {THREE.Group} - 包含所有设备的组
 */
export function createAllDeviceModels(devices, THREE, options = {}) {
  const group = new THREE.Group()

  devices.forEach(device => {
    // 只处理已设置位置的设备
    if (device.positionX !== null && device.positionY !== null) {
      const deviceModel = createDevice3DModel(device, THREE, options)
      group.add(deviceModel)
    }
  })

  return group
}

/**
 * 更新设备状态
 * @param {THREE.Group} deviceGroup - 设备组
 * @param {Object} newStatus - 新状态数据
 */
export function updateDeviceStatus(deviceGroup, newStatus) {
  deviceGroup.children.forEach(child => {
    if (child.userData.device && child.userData.device.id === newStatus.id) {
      // 移除旧模型，创建新模型
      const parent = deviceGroup.parent
      const newModel = createDevice3DModel(newStatus)
      parent.remove(deviceGroup)
      parent.add(newModel)
    }
  })
}

/**
 * 动画更新（呼吸灯效果）
 * @param {THREE.Group} devicesGroup - 设备组
 * @param {number} time - 当前时间
 */
export function animateDevices(devicesGroup, time) {
  devicesGroup.children.forEach(deviceGroup => {
    if (deviceGroup.userData.animated && deviceGroup.userData.device) {
      const device = deviceGroup.userData.device
      const isOnline = device.status === 'ONLINE'

      if (isOnline) {
        // 呼吸灯效果
        const phase = deviceGroup.userData.animationPhase || 0
        const intensity = 0.3 + Math.sin(time * 2 + phase) * 0.15

        // 更新发光强度
        deviceGroup.children.forEach(child => {
          if (child.material && child.material.emissiveIntensity !== undefined) {
            child.material.emissiveIntensity = intensity
          }
        })
      }
    }
  })
}

/**
 * 初始化CSS2D渲染器（用于HTML标签）
 * @param {HTMLElement} container - 容器元素
 * @param {THREE.WebGLRenderer} webglRenderer - WebGL渲染器
 * @returns {CSS2DRenderer} - CSS2D渲染器实例
 */
export function initCSS2DRenderer(container, webglRenderer) {
  const renderer = new CSS2DRendererClass()
  renderer.setSize(container.clientWidth, container.clientHeight)
  renderer.domElement.style.position = 'absolute'
  renderer.domElement.style.top = '0'
  renderer.domElement.style.pointerEvents = 'none'  // 允许点击穿透
  container.appendChild(renderer.domElement)
  return renderer
}

/**
 * 处理设备点击事件
 * @param {THREE.Event} event - 点击事件
 * @param {THREE.Camera} camera - 相机
 * @param {THREE.Group} devicesGroup - 设备组
 * @param {Function} callback - 回调函数
 */
export function handleDeviceClick(event, camera, devicesGroup, callback) {
  const mouse = new ThreeJS.Vector2()
  const raycaster = new ThreeJS.Raycaster()

  const rect = event.target.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

  raycaster.setFromCamera(mouse, camera)
  const intersects = raycaster.intersectObjects(devicesGroup.children, true)

  for (let intersect of intersects) {
    // 查找包含设备数据的父级组
    let obj = intersect.object
    while (obj.parent && !obj.userData.isDevice) {
      obj = obj.parent
    }

    if (obj.userData.isDevice && obj.userData.device) {
      callback(obj.userData.device, obj)
      break
    }
  }
}

/**
 * 高亮选中设备
 * @param {THREE.Group} deviceGroup - 设备组
 * @param {boolean} highlight - 是否高亮
 */
export function highlightDevice(deviceGroup, highlight = true) {
  const highlightColor = new ThreeJS.Color(0xffff00)

  deviceGroup.children.forEach(child => {
    if (child.material && child.material.emissive) {
      if (highlight) {
        child.userData.originalEmissive = child.material.emissive.getHex()
        child.material.emissive = highlightColor
        child.material.emissiveIntensity = 0.5
      } else {
        if (child.userData.originalEmissive) {
          child.material.emissive.setHex(child.userData.originalEmissive)
        }
      }
    }
  })
}
