import api from './index'

export const authApi = {
  // 登录 - 对应后端 POST /members/login
  login(credentials) {
    // 后端使用 @RequestParam，需要传递 query 参数
    return api.post('/members/login', null, {
      params: {
        username: credentials.username,
        password: credentials.password
      }
    })
  },

  // 注册 - 对应后端 POST /members/register
  register(userData) {
    return api.post('/members/register', userData)
  },

  // 获取当前用户信息 - 对应后端 GET /members/me
  getProfile() {
    return api.get('/members/me')
  },

  // 更新用户信息 - 对应后端 PUT /members/{id}
  updateProfile(id, profileData) {
    return api.put(`/members/${id}`, profileData)
  },

  // 检查用户名是否存在 - 对应后端 GET /members/check/username
  checkUsername(username) {
    return api.get('/members/check/username', { params: { username } })
  },

  // 检查手机号是否存在 - 对应后端 GET /members/check/phone
  checkPhone(phone) {
    return api.get('/members/check/phone', { params: { phone } })
  },

  // 检查邮箱是否存在 - 对应后端 GET /members/check/email
  checkEmail(email) {
    return api.get('/members/check/email', { params: { email } })
  },

  // 获取用户分页列表 - 对应后端 GET /members/page
  getUserPage(pageNum = 1, pageSize = 10) {
    return api.get('/members/page', { params: { pageNum, pageSize } })
  },

  // 删除用户 - 对应后端 DELETE /members/{id}
  deleteUser(id) {
    return api.delete(`/members/${id}`)
  },

  // 获取所有用户 - 对应后端 GET /members
  getAllUsers() {
    return api.get('/members')
  }
}