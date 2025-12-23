import { useUserStore } from '@/store/user'

/**
 * SSE连接管理类
 * 用于订阅后端SSE推送，接收实时设备数据
 */
export class DeviceSSE {
  constructor(onMessage) {
    this.eventSource = null
    this.onMessage = onMessage
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 10
    this.reconnectDelay = 3000 // 3秒重连
  }

  /**
   * 建立SSE连接
   */
  connect() {
    const userStore = useUserStore()

    if (!userStore.isLoggedIn) {
      console.warn('[SSE] 用户未登录，无法建立SSE连接')
      return
    }

    // 关闭已有连接
    if (this.eventSource) {
      this.disconnect()
    }

    try {
      // 创建SSE连接（使用相对路径，自动添加 /api 前缀）
      this.eventSource = new EventSource('/api/sse/subscribe')

      // 监听连接成功事件
      this.eventSource.addEventListener('connected', (event) => {
        console.log('[SSE] 连接成功:', event.data)
        this.reconnectAttempts = 0
      })

      // 监听辐射数据事件
      this.eventSource.addEventListener('radiation-data', (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log('[SSE] 收到辐射数据:', data)
          this.onMessage('radiation-data', data)
        } catch (e) {
          console.error('[SSE] 解析辐射数据失败:', e)
        }
      })

      // 监听环境数据事件
      this.eventSource.addEventListener('environment-data', (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log('[SSE] 收到环境数据:', data)
          this.onMessage('environment-data', data)
        } catch (e) {
          console.error('[SSE] 解析环境数据失败:', e)
        }
      })

      // 监听告警事件
      this.eventSource.addEventListener('alert', (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log('[SSE] 收到告警数据:', data)
          this.onMessage('alert', data)
        } catch (e) {
          console.error('[SSE] 解析告警数据失败:', e)
        }
      })

      // 监听错误和连接关闭
      this.eventSource.onerror = (error) => {
        console.error('[SSE] 连接错误:', error)

        // EventSource会自动重连，但我们可以处理手动重连逻辑
        if (this.eventSource.readyState === EventSource.CLOSED) {
          console.warn('[SSE] 连接已关闭，尝试重新连接...')
          this.reconnectAttempts++

          if (this.reconnectAttempts > this.maxReconnectAttempts) {
            console.error('[SSE] 超过最大重连次数，停止重连')
            this.disconnect()
          }
        }
      }

      console.log('[SSE] 正在建立连接...')
    } catch (error) {
      console.error('[SSE] 创建连接失败:', error)
    }
  }

  /**
   * 断开SSE连接
   */
  disconnect() {
    if (this.eventSource) {
      console.log('[SSE] 断开连接')
      this.eventSource.close()
      this.eventSource = null
    }
  }

  /**
   * 获取连接状态
   */
  getReadyState() {
    if (!this.eventSource) {
      return 'CLOSED'
    }

    switch (this.eventSource.readyState) {
      case EventSource.CONNECTING:
        return 'CONNECTING'
      case EventSource.OPEN:
        return 'OPEN'
      case EventSource.CLOSED:
        return 'CLOSED'
      default:
        return 'UNKNOWN'
    }
  }

  /**
   * 检查是否已连接
   */
  isConnected() {
    return this.eventSource && this.eventSource.readyState === EventSource.OPEN
  }
}

/**
 * SSE单例管理
 * 用于全局唯一SSE连接
 */
class SSEManager {
  constructor() {
    this.sseInstance = null
    this.listeners = new Map() // eventType -> Set<callback>
  }

  /**
   * 初始化SSE连接
   */
  init() {
    if (this.sseInstance) {
      console.log('[SSE Manager] 已存在连接，无需重复初始化')
      return
    }

    this.sseInstance = new DeviceSSE((eventType, data) => {
      // 分发事件给所有监听器
      const callbacks = this.listeners.get(eventType)
      if (callbacks) {
        callbacks.forEach(callback => {
          try {
            callback(data)
          } catch (e) {
            console.error(`[SSE Manager] 回调执行失败 (${eventType}):`, e)
          }
        })
      }
    })

    this.sseInstance.connect()
  }

  /**
   * 订阅事件
   * @param {string} eventType - 事件类型: 'radiation-data', 'environment-data', 'alert'
   * @param {function} callback - 回调函数
   */
  subscribe(eventType, callback) {
    if (!this.listeners.has(eventType)) {
      this.listeners.set(eventType, new Set())
    }
    this.listeners.get(eventType).add(callback)

    console.log(`[SSE Manager] 订阅事件: ${eventType}, 当前监听器数: ${this.listeners.get(eventType).size}`)

    // 返回取消订阅函数
    return () => this.unsubscribe(eventType, callback)
  }

  /**
   * 取消订阅事件
   */
  unsubscribe(eventType, callback) {
    const callbacks = this.listeners.get(eventType)
    if (callbacks) {
      callbacks.delete(callback)
      if (callbacks.size === 0) {
        this.listeners.delete(eventType)
      }
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.sseInstance) {
      this.sseInstance.disconnect()
      this.sseInstance = null
    }
    this.listeners.clear()
  }

  /**
   * 获取连接状态
   */
  getStatus() {
    return {
      connected: this.sseInstance?.isConnected() || false,
      readyState: this.sseInstance?.getReadyState() || 'CLOSED',
      listenerCount: Array.from(this.listeners.values()).reduce((sum, set) => sum + set.size, 0)
    }
  }
}

// 导出全局单例
export const sseManager = new SSEManager()

// 便捷方法：订阅设备数据更新
export const subscribeDeviceData = (callback) => {
  return sseManager.subscribe('radiation-data', callback)
}

// 便捷方法：订阅环境数据更新
export const subscribeEnvironmentData = (callback) => {
  return sseManager.subscribe('environment-data', callback)
}

// 便捷方法：订阅告警
export const subscribeAlert = (callback) => {
  return sseManager.subscribe('alert', callback)
}
