import api from './index'

export const addressApi = {
  // 获取所有地址
  getAddresses() {
    return api.get('/addresses')
  },

  // 根据ID获取地址
  getAddressById(id) {
    return api.get(`/addresses/${id}`)
  },

  // 获取默认地址
  getDefaultAddress() {
    return api.get('/addresses/default')
  },

  // 添加地址
  addAddress(addressData) {
    return api.post('/addresses', addressData)
  },

  // 更新地址
  updateAddress(id, addressData) {
    return api.put(`/addresses/${id}`, addressData)
  },

  // 删除地址
  deleteAddress(id) {
    return api.delete(`/addresses/${id}`)
  },

  // 设置默认地址
  setDefaultAddress(id) {
    return api.put(`/addresses/${id}/default`)
  }
}