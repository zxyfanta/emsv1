<template>
  <router-view />
</template>

<script setup>
import { onMounted, onBeforeUnmount } from 'vue'
import { useUserStore } from '@/store/user'
import { sseManager } from '@/utils/sse'

const userStore = useUserStore()

// 应用启动时初始化SSE连接（如果用户已登录）
onMounted(() => {
  if (userStore.isLoggedIn) {
    console.log('[App] 用户已登录，初始化SSE连接')
    sseManager.init()
  }
})

// 应用卸载时断开SSE连接
onBeforeUnmount(() => {
  console.log('[App] 断开SSE连接')
  sseManager.disconnect()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
    'Noto Color Emoji';
}
</style>
