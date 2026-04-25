<template>
  <div class="not-found-page">
    <div class="not-found-container">
      <el-empty
        :image-size="200"
        description="页面未找到"
      >
        <template #image>
          <el-icon :size="200"><Warning /></el-icon>
        </template>
        <template #description>
          <p>抱歉，您访问的页面不存在或已被删除</p>
        </template>
        <template #default>
          <el-button type="primary" @click="$router.push('/')">
            返回首页
          </el-button>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { Warning } from '@element-plus/icons-vue'
</script>

<style scoped>
.not-found-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: var(--bg-color);
}

.not-found-container {
  padding: var(--space-xl);
}
</style>