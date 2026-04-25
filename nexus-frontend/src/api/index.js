import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

// 创建axios实例
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)


// 响应拦截器
api.interceptors.response.use(
  (response) => {
    // 后端返回格式: {code, message, data}
    // 直接返回 data 字段（真正的业务数据）
    const result = response.data
    if (result.code === 200) {
      return result.data
    } else {
      // 业务错误
      ElMessage.error(result.message || '操作失败')
      return Promise.reject(new Error(result.message || '操作失败'))
    }
  },
  (error) => {
    const { response } = error

    if (response) {
      const { status, data } = response

      switch (status) {
        case 400:
          ElMessage.error(data.message || '请求参数错误')
          break
        case 401:
          // 401 错误不显示消息，由具体业务处理
          console.log('认证失败，token可能已过期')
          break
        case 403:
          ElMessage.error('权限不足')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error(data.message || '服务器内部错误')
          break
        default:
          ElMessage.error(`请求失败: ${status}`)
      }
    } else {
      // 网络错误或超时
      ElMessage.error('网络连接失败，请检查网络设置')
    }

    return Promise.reject(error)
  }
)

export default api