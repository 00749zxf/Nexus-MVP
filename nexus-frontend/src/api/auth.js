import api from './index'

export const authApi = {
  login(credentials) {
    return api.post('/members/login', {
      username: credentials.username,
      password: credentials.password
    })
  },

  register(userData) {
    return api.post('/members/register', userData)
  },

  getProfile() {
    return api.get('/members/me')
  },

  updateProfile(id, profileData) {
    return api.put(`/members/${id}`, profileData)
  },

  checkUsername(username) {
    return api.get('/members/check/username', { params: { username } })
  },

  checkPhone(phone) {
    return api.get('/members/check/phone', { params: { phone } })
  },

  checkEmail(email) {
    return api.get('/members/check/email', { params: { email } })
  },

  getUserPage(pageNum = 1, pageSize = 10) {
    return api.get('/members/page', { params: { pageNum, pageSize } })
  },

  deleteUser(id) {
    return api.delete(`/members/${id}`)
  },

  getAllUsers() {
    return api.get('/members')
  }
}
