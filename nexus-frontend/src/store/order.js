import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { orderApi } from '@/api/order'

export const useOrderStore = defineStore('order', () => {
  // 订单列表
  const orders = ref([])
  const currentOrder = ref(null)
  const isLoading = ref(false)

  // 加载订单列表
  const loadOrders = async (params = {}) => {
    try {
      isLoading.value = true
      // 调用API获取订单列表
      const response = await orderApi.getOrders(params)
      // 后端返回 OrderVO 格式，需要转换为前端格式
      orders.value = response.map(transformOrderVO)
    } catch (error) {
      console.error('加载订单列表失败:', error)
      // 如果API失败，使用空数组
      orders.value = []
    } finally {
      isLoading.value = false
    }
  }

  // 获取单个订单详情
  const getOrderById = async (id) => {
    try {
      isLoading.value = true
      // 调用API获取订单详情
      const response = await orderApi.getOrderById(id)
      currentOrder.value = transformOrderVO(response)
      return currentOrder.value
    } catch (error) {
      console.error('获取订单详情失败:', error)
      ElMessage.error('获取订单详情失败')
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 创建订单
  const createOrder = async (orderData) => {
    try {
      isLoading.value = true

      // 调用API创建订单 - 直接传递Checkout构建的数据
      const response = await orderApi.createOrder(orderData)
      const newOrder = transformOrderVO(response)

      // 添加到订单列表
      orders.value.unshift(newOrder)
      currentOrder.value = newOrder

      ElMessage.success('订单创建成功')
      return newOrder
    } catch (error) {
      console.error('创建订单失败:', error)
      ElMessage.error('创建订单失败')
      throw error
    } finally {
      isLoading.value = false
    }
  }

  // 取消订单
  const cancelOrder = async (orderId) => {
    try {
      await orderApi.cancelOrder(orderId)

      // 更新本地订单状态
      const order = orders.value.find(o => o.id === orderId)
      if (order) {
        order.status = 'cancelled'
        order.statusText = '已取消'
        order.cancelledAt = new Date().toISOString().replace('T', ' ').substr(0, 19)
      }

      if (currentOrder.value && currentOrder.value.id === orderId) {
        currentOrder.value.status = 'cancelled'
        currentOrder.value.statusText = '已取消'
      }

      ElMessage.success('订单已取消')
    } catch (error) {
      console.error('取消订单失败:', error)
      ElMessage.error('取消订单失败')
      throw error
    }
  }

  // 删除订单
  const deleteOrder = async (orderId) => {
    try {
      await orderApi.deleteOrder(orderId)
      orders.value = orders.value.filter(o => o.id !== orderId)

      if (currentOrder.value && currentOrder.value.id === orderId) {
        currentOrder.value = null
      }

      ElMessage.success('订单已删除')
    } catch (error) {
      console.error('删除订单失败:', error)
      ElMessage.error('删除订单失败')
      throw error
    }
  }

  // 确认收货
  const confirmReceipt = async (orderId) => {
    try {
      await orderApi.confirmReceipt(orderId)

      // 更新本地订单状态
      const order = orders.value.find(o => o.id === orderId)
      if (order) {
        order.status = 'completed'
        order.statusText = '已完成'
        order.completedAt = new Date().toISOString().replace('T', ' ').substr(0, 19)
      }

      if (currentOrder.value && currentOrder.value.id === orderId) {
        currentOrder.value.status = 'completed'
        currentOrder.value.statusText = '已完成'
      }

      ElMessage.success('确认收货成功')
    } catch (error) {
      console.error('确认收货失败:', error)
      ElMessage.error('确认收货失败')
      throw error
    }
  }

  // 支付订单
  const payOrder = async (orderId) => {
    try {
      await orderApi.payOrder(orderId)

      // 更新本地订单状态
      const order = orders.value.find(o => o.id === orderId)
      if (order) {
        order.status = 'paid'
        order.statusText = '待发货'
      }

      if (currentOrder.value && currentOrder.value.id === orderId) {
        currentOrder.value.status = 'paid'
        currentOrder.value.statusText = '待发货'
      }

      ElMessage.success('支付成功')
    } catch (error) {
      console.error('支付失败:', error)
      ElMessage.error('支付失败')
      throw error
    }
  }

  // 获取订单数量统计
  const getOrderCount = async (status) => {
    try {
      const count = await orderApi.getOrderCount(status)
      return count
    } catch (error) {
      console.error('获取订单数量失败:', error)
      return 0
    }
  }

  // 转换后端OrderVO为前端格式
  const transformOrderVO = (orderVO) => {
    return {
      id: orderVO.id || orderVO.orderSn,
      orderSn: orderVO.orderSn,
      status: orderVO.status,
      statusText: getStatusText(orderVO.status),
      totalAmount: orderVO.totalAmount,
      itemCount: orderVO.totalQuantity || (orderVO.orderItems ? orderVO.orderItems.length : 0),
      createdAt: orderVO.createTime || orderVO.createdAt,
      updatedAt: orderVO.updateTime || orderVO.updatedAt,
      items: (orderVO.orderItems || []).map(item => ({
        productId: item.productId,
        name: item.productName,
        image: item.productImage || '/images/product-placeholder.jpg',
        price: item.productPrice,
        quantity: item.quantity,
        totalPrice: item.totalPrice,
        specs: [] // 后端暂不支持规格
      })),
      shippingAddress: parseAddress(orderVO.receiverAddress, orderVO.receiverName, orderVO.receiverPhone)
    }
  }

  // 解析地址字符串
  const parseAddress = (addressStr, name, phone) => {
    return {
      name: name || '',
      phone: phone || '',
      province: '',
      city: '',
      district: '',
      detail: addressStr || '',
      postalCode: ''
    }
  }

  // 获取状态文本
  const getStatusText = (status) => {
    const statusMap = {
      0: '待付款',
      1: '待发货',
      2: '已发货',
      3: '已完成',
      4: '已取消'
    }
    return statusMap[status] || '未知状态'
  }

  // 获取订单状态选项
  const getStatusOptions = () => {
    return [
      { value: 'all', label: '全部' },
      { value: 0, label: '待付款' },
      { value: 1, label: '待发货' },
      { value: 2, label: '已发货' },
      { value: 3, label: '已完成' },
      { value: 4, label: '已取消' }
    ]
  }

  return {
    orders,
    currentOrder,
    isLoading,
    loadOrders,
    getOrderById,
    createOrder,
    cancelOrder,
    confirmReceipt,
    deleteOrder,
    payOrder,
    getOrderCount,
    getStatusOptions
  }
})