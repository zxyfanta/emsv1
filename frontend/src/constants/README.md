# 地区轮廓3D可视化 - 使用说明

## 概述

本模块实现了在3D场景中显示地区轮廓的功能，支持复杂多边形、内部空洞、自定义样式等特性。

## 文件结构

```
src/constants/
├── regionOutline.js          # 核心实现文件
├── regionOutlineExamples.js  # 示例数据文件
└── README.md                 # 本说明文档
```

## 快速开始

### 1. 基本使用

```javascript
// 在组件中导入
import { createRegionOutline, regionOutlineData } from '@/constants/regionOutline'

// 创建轮廓
const outline = createRegionOutline(scene, regionOutlineData)
```

### 2. 数据格式

```javascript
const myRegion = {
  name: '我的地区',
  // 外轮廓点（顺时针方向）
  outerBoundary: [
    [x1, z1],  // 点1
    [x2, z2],  // 点2
    // ... 更多点
  ],
  // 内部空洞（可选，逆时针方向）
  holes: [
    [
      [x1, z1],  // 空洞1的点
      [x2, z2],
      // ...
    ]
  ],
  // 样式配置
  style: {
    height: 2,           // 拉伸高度
    color: 0x1890ff,     // 边界线颜色（十六进制）
    fillColor: 0x1565c0, // 填充颜色
    opacity: 0.6,        // 透明度（0-1）
    lineWidth: 3         // 边界线宽度
  }
}
```

## 坐标系统

### 3D场景坐标系

```
        +Z (前方)
         ↑
         |
         |
-X ←-----+-----→ +X (右方)
         |
         |
         ↓
        -Z (后方)
```

### 坐标转换

如果需要从实际地图坐标转换为场景坐标：

```javascript
// 假设实际地图区域为 1000m × 800m
// 场景坐标系范围为 -50 到 50（100单位）

const scale = 0.1  // 缩放比例
const offsetX = -500  // X轴偏移
const offsetZ = -400  // Z轴偏移

function convertToSceneCoords(realX, realZ) {
  return [
    (realX + offsetX) * scale,
    (realZ + offsetZ) * scale
  ]
}

// 示例：实际坐标 (200, 100) → 场景坐标 [-30, -30]
const sceneCoords = convertToSceneCoords(200, 100)
```

## API 文档

### createRegionOutline(scene, outlineData)

创建地区轮廓3D对象。

**参数：**
- `scene` (THREE.Scene): Three.js场景对象
- `outlineData` (Object): 轮廓数据对象

**返回：**
- `THREE.Group`: 包含所有轮廓3D对象的组

**渲染内容：**
1. 立体区域（ExtrudeGeometry）
2. 顶部边界线
3. 底部轮廓线（在地面上）
4. 内部空洞边界线（如果有）

### updateRegionOutline(group, outlineData)

更新已有轮廓。

**参数：**
- `group` (THREE.Group): 要更新的轮廓组
- `outlineData` (Object): 新的轮廓数据

**返回：**
- `THREE.Group`: 新的轮廓组

## 示例

### 示例1：简单矩形

```javascript
const rectangle = {
  name: '矩形区域',
  outerBoundary: [
    [-20, -15],
    [20, -15],
    [20, 15],
    [-20, 15]
  ],
  holes: [],
  style: {
    height: 2,
    color: 0x1890ff,
    fillColor: 0x1565c0,
    opacity: 0.6
  }
}

const outline = createRegionOutline(scene, rectangle)
```

### 示例2：带空洞的区域

```javascript
const ring = {
  name: '环形区域',
  outerBoundary: [
    [0, -25],
    [25, 0],
    [0, 25],
    [-25, 0]
  ],
  holes: [
    [
      [0, -10],
      [10, 0],
      [0, 10],
      [-10, 0]
    ]
  ],
  style: {
    height: 2,
    color: 0xff4444,
    fillColor: 0xaa0000,
    opacity: 0.5
  }
}
```

### 示例3：动态切换轮廓

```javascript
import { factoryRegion, circularRegion } from '@/constants/regionOutlineExamples'

// 创建初始轮廓
let currentOutline = createRegionOutline(scene, factoryRegion)

// 切换到圆形区域
function switchToCircle() {
  scene.remove(currentOutline)
  currentOutline = createRegionOutline(scene, circularRegion)
}
```

## 从地图导入轮廓

### 方法1：使用 GeoJSON

1. 访问 [geojson.io](http://geojson.io/)
2. 使用绘制工具创建多边形
3. 导出 GeoJSON
4. 转换坐标：

```javascript
// GeoJSON 坐标格式：[longitude, latitude]
// 转换为场景坐标：[x, z]

function geojsonToSceneCoords(coordinates) {
  return coordinates.map(([lon, lat]) => {
    // 根据实际范围调整缩放和偏移
    const x = (lon - minLon) * scaleX - offsetX
    const z = (lat - minLat) * scaleZ - offsetZ
    return [x, z]
  })
}
```

### 方法2：使用 CAD 软件

1. 在 AutoCAD/SketchUp 中绘制
2. 导出关键点坐标
3. 格式化为数组：

```javascript
const fromCAD = [
  [0, 0],
  [100, 0],
  [100, 50],
  [0, 50]
]
```

## 样式定制

### 颜色参考

```javascript
// 蓝色系（科技感）
color: 0x1890ff      // 亮蓝色
fillColor: 0x1565c0  // 深蓝色

// 绿色系（环保）
color: 0x52c41a      // 亮绿色
fillColor: 0x389e0d  // 深绿色

// 橙色系（警告）
color: 0xfa8c16      // 亮橙色
fillColor: 0xd46b08  // 深橙色

// 红色系（危险）
color: 0xff4444      // 亮红色
fillColor: 0xaa0000  // 深红色
```

### 透明度建议

```javascript
opacity: 0.3  // 非常透明，适合背景
opacity: 0.5  // 中等透明，推荐值
opacity: 0.7  // 较不透明，适合突出显示
opacity: 1.0  // 完全不透明
```

## 性能优化

### 大型轮廓优化

对于点数很多的大型轮廓：

```javascript
// 1. 简化轮廓（减少点数）
function simplifyOutline(points, tolerance = 1) {
  // 使用 Douglas-Peucker 算法简化
  // ... 实现略
}

// 2. 分段渲染
function createLargeOutline(points) {
  const segments = []
  const segmentSize = 100

  for (let i = 0; i < points.length; i += segmentSize) {
    const segment = points.slice(i, i + segmentSize)
    segments.push(createSegment(segment))
  }

  return segments
}
```

## 常见问题

### Q: 轮廓显示不正确？

A: 检查以下几点：
- 外轮廓点是否按顺时针顺序排列
- 内部空洞点是否按逆时针顺序排列
- 坐标是否在合理范围内（-50到50）

### Q: 如何调整轮廓高度？

A: 修改 `style.height` 属性：

```javascript
style: {
  height: 5  // 设置为5单位高度
}
```

### Q: 可以添加多个轮廓吗？

A: 可以，多次调用 `createRegionOutline`：

```javascript
const outline1 = createRegionOutline(scene, region1)
const outline2 = createRegionOutline(scene, region2)
const outline3 = createRegionOutline(scene, region3)
```

### Q: 如何让轮廓可点击？

A: 使用 Raycaster 检测点击：

```javascript
const raycaster = new THREE.Raycaster()
const mouse = new THREE.Vector2()

function onMouseClick(event) {
  mouse.x = (event.clientX / width) * 2 - 1
  mouse.y = -(event.clientY / height) * 2 + 1

  raycaster.setFromCamera(mouse, camera)
  const intersects = raycaster.intersectObjects(outline.children)

  if (intersects.length > 0) {
    console.log('点击了轮廓！', intersects[0])
  }
}
```

## 扩展开发

### 添加标签

```javascript
function createLabel(text, position) {
  const canvas = document.createElement('canvas')
  const context = canvas.getContext('2d')
  // ... 绘制文本

  const texture = new THREE.CanvasTexture(canvas)
  const material = new THREE.SpriteMaterial({ map: texture })
  const sprite = new THREE.Sprite(material)
  sprite.position.set(...position)
  return sprite
}

// 使用
const label = createLabel('区域A', [0, 5, 0])
outline.add(label)
```

### 添加动画

```javascript
function animateOutline(outline, speed = 0.001) {
  let time = 0

  function animate() {
    time += speed
    outline.position.y = Math.sin(time) * 0.5
    requestAnimationFrame(animate)
  }

  animate()
}
```

## 技术支持

如有问题，请查看：
- Three.js 官方文档：https://threejs.org/docs/
- DataV 文档：http://data.jiaminghi.com/
