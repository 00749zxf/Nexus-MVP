<template>
  <div id="app">
    <!-- 路由视图 -->
    <router-view />
  </div>
</template>

<script setup>
// 应用入口，所有逻辑通过路由和状态管理处理
</script>

<style scoped>
#app {
  min-height: 100vh;
  background-color: var(--bg-color);
}
</style>