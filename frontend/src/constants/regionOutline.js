/**
 * 地区轮廓数据
 * 使用一组经纬度坐标点定义地区边界
 * 坐标格式：[x, z]（对应3D场景中的平面坐标）
 */
import * as THREE from 'three'

export const regionOutlineData = {
  name: '示例地区轮廓',
  // 外轮廓点（顺时针方向）
  outerBoundary: [
    [-20, -20],  // 左下
    [0, -25],    // 下中
    [20, -20],   // 右下
    [25, 0],     // 右中
    [20, 20],    // 右上
    [0, 25],     // 上中
    [-20, 20],   // 左上
    [-25, 0]     // 左中
  ],
  // 内部空洞（可选，逆时针方向）
  holes: [
    // [
    //   [-5, -5],
    //   [5, -5],
    //   [5, 5],
    //   [-5, 5]
    // ]
  ],
  // 轮廓样式配置
  style: {
    height: 2,           // 拉伸高度
    color: 0x1890ff,     // 边界线颜色
    fillColor: 0x1565c0, // 填充颜色
    opacity: 0.6,        // 填充透明度
    lineWidth: 3         // 边界线宽度
  }
}

/**
 * 创建地区轮廓3D对象
 * @param {THREE.Scene} scene - Three.js场景对象
 * @param {Object} outlineData - 轮廓数据
 * @returns {THREE.Group} - 包含轮廓3D对象的组
 */
export function createRegionOutline(scene, outlineData = regionOutlineData) {
  const group = new THREE.Group()
  const { outerBoundary, holes, style } = outlineData

  // 1. 创建形状
  const shape = new THREE.Shape()

  // 绘制外轮廓
  if (outerBoundary.length > 0) {
    shape.moveTo(outerBoundary[0][0], outerBoundary[0][1])
    for (let i = 1; i < outerBoundary.length; i++) {
      shape.lineTo(outerBoundary[i][0], outerBoundary[i][1])
    }
    shape.closePath()
  }

  // 绘制内部空洞
  if (holes && holes.length > 0) {
    holes.forEach(hole => {
      const holePath = new THREE.Path()
      if (hole.length > 0) {
        holePath.moveTo(hole[0][0], hole[0][1])
        for (let i = 1; i < hole.length; i++) {
          holePath.lineTo(hole[i][0], hole[i][1])
        }
        holePath.closePath()
      }
      shape.holes.push(holePath)
    })
  }

  // 2. 创建拉伸几何体（立体区域）
  const extrudeSettings = {
    steps: 1,
    depth: style.height || 2,
    bevelEnabled: true,
    bevelThickness: 0.2,
    bevelSize: 0.1,
    bevelSegments: 2
  }

  const geometry = new THREE.ExtrudeGeometry(shape, extrudeSettings)

  // 3. 创建材质
  const material = new THREE.MeshStandardMaterial({
    color: style.fillColor || 0x1565c0,
    transparent: true,
    opacity: style.opacity || 0.6,
    metalness: 0.3,
    roughness: 0.7,
    side: THREE.DoubleSide
  })

  const mesh = new THREE.Mesh(geometry, material)
  mesh.rotation.x = -Math.PI / 2 // 旋转到水平面
  mesh.position.y = 0.1 // 稍微抬高，避免z-fighting
  group.add(mesh)

  // 4. 创建边界线（顶部）
  const edgesGeometry = new THREE.EdgesGeometry(geometry)
  const edgesMaterial = new THREE.LineBasicMaterial({
    color: style.color || 0x1890ff,
    linewidth: style.lineWidth || 3
  })
  const edges = new THREE.LineSegments(edgesGeometry, edgesMaterial)
  edges.rotation.x = -Math.PI / 2
  edges.position.y = 0.1
  group.add(edges)

  // 5. 创建底部轮廓线（在地面上）
  const points = outerBoundary.map(point => new THREE.Vector3(point[0], 0.05, point[1]))
  points.push(points[0].clone()) // 闭合轮廓

  const lineGeometry = new THREE.BufferGeometry().setFromPoints(points)
  const lineMaterial = new THREE.LineBasicMaterial({
    color: style.color || 0x1890ff,
    linewidth: style.lineWidth || 3
  })
  const bottomLine = new THREE.Line(lineGeometry, lineMaterial)
  group.add(bottomLine)

  // 6. 添加内部空洞边界线（如果有）
  if (holes && holes.length > 0) {
    holes.forEach(hole => {
      const holePoints = hole.map(point => new THREE.Vector3(point[0], 0.05, point[1]))
      holePoints.push(holePoints[0].clone())

      const holeLineGeometry = new THREE.BufferGeometry().setFromPoints(holePoints)
      const holeLine = new THREE.Line(holeLineGeometry, lineMaterial)
      group.add(holeLine)
    })
  }

  scene.add(group)
  return group
}

/**
 * 更新地区轮廓
 * @param {THREE.Group} group - 要更新的轮廓组
 * @param {Object} outlineData - 新的轮廓数据
 */
export function updateRegionOutline(group, outlineData) {
  // 移除旧对象
  while(group.children.length > 0){
    const child = group.children[0]
    if (child.geometry) child.geometry.dispose()
    if (child.material) {
      if (Array.isArray(child.material)) {
        child.material.forEach(m => m.dispose())
      } else {
        child.material.dispose()
      }
    }
    group.remove(child)
  }

  // 重新创建（需要传入scene对象）
  const scene = group.parent
  scene.remove(group)
  return createRegionOutline(scene, outlineData)
}
