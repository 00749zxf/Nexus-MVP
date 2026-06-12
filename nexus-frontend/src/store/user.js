import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api/index'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(null)
  const loading = ref(false)

  // 计算属性
  const isAuthenticated = computed(() => !!token.value)

  // 登录
  const login = async (credentials) => {
    loading.value = true
    try {
      const { username, password } = credentials

      const tokenValue = await api.post('/members/login', { username, password })

      // 拦截器已返回 result.data，所以 tokenValue 就是 JWT token
      token.value = tokenValue
      localStorage.setItem('token', tokenValue)

      // 获取用户信息
      await loadUser()

      ElMessage.success('登录成功')
      return user.value
    } catch (error) {
      console.error('登录失败:', error)
      const message = error.response?.data?.message || error.message || '登录失败，请检查用户名和密码'
      ElMessage.error(message)
      throw new Error(message)
    } finally {
      loading.value = false
    }
  }


  // 注册
  const register = async (userData) => {
    loading.value = true
    try {
      await api.post('/members/register', userData)
      ElMessage.success('注册成功')

      // 注册成功后自动登录
      try {
        const tokenValue = await api.post('/members/login', {
          username: userData.username,
          password: userData.password
        })

        token.value = tokenValue
        localStorage.setItem('token', tokenValue)

        await loadUser()
        ElMessage.success('自动登录成功')
      } catch (loginError) {
        console.warn('注册成功但自动登录失败:', loginError)
        ElMessage.info('注册成功，请手动登录')
      }
    } catch (error) {
      const message = error.response?.data?.message || error.message || '注册失败'
      ElMessage.error(message)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 退出登录
  const logout = () => {
    user.value = null
    token.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    ElMessage.success('已退出登录')
  }

  // 加载用户信息
  const loadUser = async () => {
    const savedToken = localStorage.getItem('token')

    if (!savedToken) {
      token.value = ''
      return
    }

    token.value = savedToken

    try {
      const userData = await api.get('/members/me')
      user.value = userData
      localStorage.setItem('user', JSON.stringify(userData))
    } catch (error) {
      console.error('获取用户信息失败:', error)
      if (error.response?.status === 401) {
        logout()
      }
    }
  }

  // 更新用户信息
  const updateProfile = async (profileData) => {
    try {
      const userData = await api.put(`/members/${user.value.id}`, profileData)
      user.value = userData
      localStorage.setItem('user', JSON.stringify(userData))
      ElMessage.success('个人信息更新成功')
    } catch (error) {
      const message = error.response?.data?.message || error.message || '更新失败'
      ElMessage.error(message)
      throw error
    }
  }

  // 初始化
  const initialize = () => {
    loadUser()
  }

  return {
    user,
    token,
    loading,
    isAuthenticated,
    login,
    register,
    logout,
    updateProfile,
    initialize
  }
})
