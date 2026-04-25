import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import agentApi from '@/agent/api/agentApi'

/**
 * Agent状态管理
 */
export const useAgentStore = defineStore('agent', () => {
  // 会话ID
  const sessionId = ref(null)

  // 消息列表
  const messages = ref([])

  // 是否正在加载
  const isLoading = ref(false)

  // Agent类型
  const agentType = ref('CUSTOMER_SERVICE')

  // 建议操作
  const suggestions = ref([])

  // 是否需要转人工
  const needHumanSupport = ref(false)

  // 上下文信息
  const context = ref({})

  // 消息数量
  const messageCount = computed(() => messages.value.length)

  /**
   * 发送消息
   */
  async function sendMessage(content, extraContext = {}) {
    if (!content.trim()) return

    // 添加用户消息
    messages.value.push({
      id: Date.now(),
      role: 'USER',
      content: content,
      timestamp: new Date()
    })

    isLoading.value = true

    try {
      const response = await agentApi.chat({
        sessionId: sessionId.value,
        agentType: agentType.value,
        message: content,
        context: {
          ...context.value,
          ...extraContext
        }
      })

      // 更新会话ID
      sessionId.value = response.sessionId

      // 添加Agent回复
      messages.value.push({
        id: Date.now() + 1,
        role: 'AGENT',
        content: response.response,
        timestamp: new Date(),
        toolCalls: response.toolCalls
      })

      // 更新建议
      suggestions.value = response.suggestions || []
      needHumanSupport.value = response.needHumanSupport

      return response

    } catch (error) {
      // 添加错误消息
      messages.value.push({
        id: Date.now() + 1,
        role: 'AGENT',
        content: '抱歉，处理您的请求时出现错误，请稍后再试。',
        timestamp: new Date(),
        isError: true
      })
      throw error

    } finally {
      isLoading.value = false
    }
  }

  /**
   * 清空会话
   */
  function clearSession() {
    sessionId.value = null
    messages.value = []
    suggestions.value = []
    needHumanSupport.value = false
  }

  /**
   * 设置上下文
   */
  function setContext(newContext) {
    context.value = { ...context.value, ...newContext }
  }

  /**
   * 设置Agent类型
   */
  function setAgentType(type) {
    agentType.value = type
    clearSession()
  }

  return {
    sessionId,
    messages,
    isLoading,
    agentType,
    suggestions,
    needHumanSupport,
    context,
    messageCount,
    sendMessage,
    clearSession,
    setContext,
    setAgentType
  }
})