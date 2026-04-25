<template>
  <div class="agent-message" :class="{ 'user-message': message.role === 'USER', 'error-message': message.isError }">
    <div class="message-avatar">
      <el-avatar v-if="message.role === 'USER'" :size="32" class="user-avatar">
        <el-icon><User /></el-icon>
      </el-avatar>
      <el-avatar v-else :size="32" class="agent-avatar">
        <el-icon><Service /></el-icon>
      </el-avatar>
    </div>
    <div class="message-content">
      <div class="message-header">
        <span class="message-role">{{ message.role === 'USER' ? '我' : '智能助手' }}</span>
        <span class="message-time">{{ formatTime(message.timestamp) }}</span>
      </div>
      <div class="message-text" v-html="formatContent(message.content)"></div>
      <!-- 工具调用信息 -->
      <div v-if="message.toolCalls && message.toolCalls.length > 0" class="tool-calls">
        <el-tag v-for="tool in message.toolCalls" :key="tool.toolName" size="small" :type="tool.success ? 'success' : 'danger'">
          {{ tool.toolName }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { User, Service } from '@element-plus/icons-vue'

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
})

function formatTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function formatContent(content) {
  if (!content) return ''
  // 简单的Markdown渲染：处理粗体和换行
  return content
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.agent-message {
  display: flex;
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 8px;
  background: #f5f7fa;
}

.user-message {
  background: #ecf5ff;
  flex-direction: row-reverse;
}

.error-message {
  background: #fef0f0;
}

.message-avatar {
  margin-right: 12px;
}

.user-message .message-avatar {
  margin-right: 0;
  margin-left: 12px;
}

.user-avatar {
  background: #409eff;
}

.agent-avatar {
  background: #67c23a;
}

.message-content {
  max-width: 70%;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.message-role {
  font-weight: 500;
  color: #303133;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.message-text {
  color: #606266;
  line-height: 1.6;
}

.tool-calls {
  margin-top: 8px;
}

.tool-calls .el-tag {
  margin-right: 4px;
}
</style>