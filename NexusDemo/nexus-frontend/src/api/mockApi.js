import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建模拟axios实例
const mockApi = axios.create({
  baseURL: '/api',  // 使用相对路径
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 模拟用户数据
const mockUsers = [
  {
    id: 1,
    username: 'admin',
    email: 'admin@example.com',
    avatar: '/images/user-avatar.jpg',
    phone: '13800138000',
    address: '北京市朝阳区',
    balance: 10000
  },
  {
    id: 2,
    username: 'user',
    email: 'user@example.com',
    avatar: '/images/user-avatar.jpg',
    phone: '13800138001',
    address: '上海市浦东新区',
    balance: 5000
  }
]

// 模拟数据库
const mockDatabase = {
  // 登录
  '/auth/login': {
    'POST': (data) => {
      const { username, password } = data

      if (username === 'admin' && password === '123456') {
        const user = mockUsers[0]
        return {
          user,
          token: `mock-token-${Date.now()}-${user.id}`
        }
      } else if (username === 'user' && password === '123456') {
        const user = mockUsers[1]
        return {
          user,
          token: `mock-token-${Date.now()}-${user.id}`
        }
      } else {
        const error = new Error('用户名或密码错误')
        error.response = { status: 401, data: { message: '用户名或密码错误' } }
        throw error
      }
    }
  },

  // 注册
  '/auth/register': {
    'POST': (data) => {
      return { success: true, message: '注册成功' }
    }
  },

  // 获取用户信息
  '/user/profile': {
    'GET': (config) => {
      const token = config.headers?.Authorization?.replace('Bearer ', '')
      if (!token) {
        const error = new Error('未授权')
        error.response = { status: 401, data: { message: '未授权' } }
        throw error
      }

      // 从token中提取用户ID（模拟）
      const userId = parseInt(token.split('-')[2])
      const user = mockUsers.find(u => u.id === userId)

      if (!user) {
        const error = new Error('用户不存在')
        error.response = { status: 404, data: { message: '用户不存在' } }
        throw error
      }

      return user
    }
  }
}

// 模拟响应拦截器
mockApi.interceptors.response.use(
  (response) => {
    return response.data
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
          ElMessage.error('未授权，请重新登录')
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
      ElMessage.error('网络连接失败，请检查网络设置')
    }

    return Promise.reject(error)
  }
)

// 重写request方法，实现模拟API
mockApi.request = function(config) {
  const { method, url, data, headers } = config

  // 检查是否有对应的模拟路由
  const route = mockDatabase[url]
  if (route && route[method]) {
    try {
      // 模拟响应延迟
      return new Promise((resolve) => {
        setTimeout(() => {
          const response = route[method]({
            data,
            headers
          })
          resolve({ data: response })
        }, 500) // 500ms延迟
      })
    } catch (error) {
      // 如果模拟函数抛出错误，返回错误响应
      const errorResponse = error.response || { status: 500, data: { message: '服务器错误' } }
      const errorObj = new Error(error.message)
      errorObj.response = errorResponse
      throw errorObj
    }
  }

  // 如果没有对应的模拟路由，返回404错误
  const error = new Error('API端点不存在')
  error.response = { status: 404, data: { message: 'API端点不存在' } }
  throw error
}

export default mockApi