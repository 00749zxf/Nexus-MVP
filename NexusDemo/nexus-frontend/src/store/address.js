import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { addressApi } from '@/api/address'

export const useAddressStore = defineStore('address', () => {
  // 状态
  const addresses = ref([])
  const defaultAddress = ref(null)
  const loading = ref(false)

  // 计算属性
  const hasAddresses = computed(() => addresses.value.length > 0)

  // 加载地址列表
  const loadAddresses = async () => {
    loading.value = true
    try {
      addresses.value = await addressApi.getAddresses()
      // 找到默认地址
      defaultAddress.value = addresses.value.find(a => a.isDefault) || addresses.value[0]
    } catch (error) {
      console.error('加载地址失败:', error)
      addresses.value = []
    } finally {
      loading.value = false
    }
  }

  // 获取单个地址
  const getAddressById = async (id) => {
    try {
      return await addressApi.getAddressById(id)
    } catch (error) {
      console.error('获取地址失败:', error)
      return null
    }
  }

  // 获取默认地址
  const getDefaultAddress = async () => {
    try {
      const address = await addressApi.getDefaultAddress()
      defaultAddress.value = address
      return address
    } catch (error) {
      console.error('获取默认地址失败:', error)
      return null
    }
  }

  // 添加地址
  const addAddress = async (addressData) => {
    try {
      await addressApi.addAddress(addressData)
      ElMessage.success('地址添加成功')
      await loadAddresses()
    } catch (error) {
      ElMessage.error(error.message || '添加地址失败')
      throw error
    }
  }

  // 更新地址
  const updateAddress = async (id, addressData) => {
    try {
      await addressApi.updateAddress(id, addressData)
      ElMessage.success('地址更新成功')
      await loadAddresses()
    } catch (error) {
      ElMessage.error(error.message || '更新地址失败')
      throw error
    }
  }

  // 删除地址
  const deleteAddress = async (id) => {
    try {
      await addressApi.deleteAddress(id)
      ElMessage.success('地址删除成功')
      await loadAddresses()
    } catch (error) {
      ElMessage.error(error.message || '删除地址失败')
      throw error
    }
  }

  // 设置默认地址
  const setDefaultAddress = async (id) => {
    try {
      await addressApi.setDefaultAddress(id)
      ElMessage.success('已设为默认地址')
      await loadAddresses()
    } catch (error) {
      ElMessage.error(error.message || '设置默认地址失败')
      throw error
    }
  }

  return {
    addresses,
    defaultAddress,
    loading,
    hasAddresses,
    loadAddresses,
    getAddressById,
    getDefaultAddress,
    addAddress,
    updateAddress,
    deleteAddress,
    setDefaultAddress
  }
})