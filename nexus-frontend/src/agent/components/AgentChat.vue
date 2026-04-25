<template>
  <div class="agent-chat">
    <!-- 消息列表（固定高度，可滚动） -->
    <div class="messages-container" ref="messagesRef">
      <div class="messages">
        <!-- 欢迎消息 -->
        <AgentMessage v-if="messages.length === 0" :message="welcomeMessage" />
        <!-- 消息列表 -->
        <AgentMessage v-for="msg in messages" :key="msg.id" :message="msg" />
        <!-- 加载状态 -->
        <div v-if="isLoading" class="loading-message">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在思考...</span>
        </div>
      </div>
    </div>

    <!-- 建议操作（可选显示） -->
    <div v-if="suggestions.length > 0 && !isLoading" class="suggestions-bar">
      <AgentSuggestions :items="suggestions" @select="onSuggestionSelect" />
    </div>

    <!-- 输入框（固定底部） -->
    <div class="input-fixed">
      <AgentInput :loading="isLoading" :disabled="false" @submit="onSubmit" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { useAgentStore } from '@/agent/store/agent'
import { Loading } from '@element-plus/icons-vue'
import AgentMessage from './AgentMessage.vue'
import AgentInput from './AgentInput.vue'
import AgentSuggestions from './AgentSuggestions.vue'

const props = defineProps({
  type: {
    type: String,
    default: 'CUSTOMER_SERVICE'
  },
  context: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['suggestion-select'])

const agentStore = useAgentStore()
const messagesRef = ref(null)

// 欢迎消息
const welcomeMessage = computed(() => ({
  id: 'welcome',
  role: 'AGENT',
  content: '您好，我是Nexus智能客服助手。请问有什么可以帮助您的？\n\n您可以咨询商品信息、查询订单状态、了解售后政策等问题。',
  timestamp: new Date()
}))

// 从store获取状态
const messages = computed(() => agentStore.messages)
const isLoading = computed(() => agentStore.isLoading)
const suggestions = computed(() => agentStore.suggestions)

// 初始化
watch(() => props.type, (newType) => {
  agentStore.setAgentType(newType)
}, { immediate: true })

watch(() => props.context, (newContext) => {
  agentStore.setContext(newContext)
}, { immediate: true, deep: true })

// 监听消息变化，自动滚动到底部
watch(messages, async () => {
  await nextTick()
  scrollToBottom()
}, { deep: true })

// 发送消息
async function onSubmit(content) {
  await agentStore.sendMessage(content, props.context)
}

// 建议操作选择
function onSuggestionSelect(item) {
  emit('suggestion-select', item)
}

// 滚动到底部
function scrollToBottom() {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}
</script>

<style scoped>
.agent-chat {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  min-height: 0; /* 关键：允许flex收缩 */
}

.messages {
  display: flex;
  flex-direction: column;
}

.loading-message {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  color: #909399;
}

.loading-message .el-icon {
  margin-right: 8px;
  animation: rotating 2s linear infinite;
}

.suggestions-bar {
  padding: 8px 16px;
  background: #f5f7fa;
  border-top: 1px solid #e4e7ed;
}

.input-fixed {
  flex-shrink: 0; /* 固定在底部，不参与flex缩放 */
  border-top: 1px solid #e4e7ed;
}

@keyframes rotating {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>