import api from '@/api/index'

/**
 * Agent API封装
 */
export const agentApi = {
  /**
   * 与Agent对话
   */
  chat(params) {
    return api.post('/agent/chat', params)
  },

  /**
   * 获取Agent类型列表
   */
  getTypes() {
    return api.get('/agent/types')
  }
}

export default agentApi