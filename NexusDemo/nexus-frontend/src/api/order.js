import api from './index'

export const orderApi = {
  // 获取订单列表 - 对应后端 GET /orders?status=xxx
  getOrders(params = {}) {
    return api.get('/orders', { params })
  },

  // 获取订单详情 - 对应后端 GET /orders/{orderId}
  getOrderById(id) {
    return api.get(`/orders/${id}`)
  },

  // 从购物车创建订单 - 对应后端 POST /orders/from-cart
  createOrderFromCart(orderData) {
    // 后端 OrderDTO: receiverName, receiverPhone, receiverAddress, cartItemIds, productId, quantity, note
    return api.post('/orders/from-cart', {
      receiverName: orderData.shippingAddress?.name,
      receiverPhone: orderData.shippingAddress?.phone,
      receiverAddress: formatAddress(orderData.shippingAddress),
      cartItemIds: orderData.cartItemIds,
      note: orderData.note
    })
  },

  // 直接购买创建订单 - 对应后端 POST /orders/direct
  createOrderDirect(productId, quantity, orderData) {
    return api.post('/orders/direct', {
      productId,
      quantity,
      receiverName: orderData.shippingAddress?.name,
      receiverPhone: orderData.shippingAddress?.phone,
      receiverAddress: formatAddress(orderData.shippingAddress),
      note: orderData.note
    })
  },

  // 创建订单（根据购物车）- 对应后端 POST /orders/from-cart
  createOrder(orderData) {
    // 直接发送后端需要的字段格式
    // 如果orderData已经包含receiverName等字段，直接使用
    // 如果orderData包含shippingAddress对象，进行转换
    const payload = {
      receiverName: orderData.receiverName || orderData.shippingAddress?.name,
      receiverPhone: orderData.receiverPhone || orderData.shippingAddress?.phone,
      receiverAddress: orderData.receiverAddress || formatAddress(orderData.shippingAddress),
      cartItemIds: orderData.cartItemIds,
      note: orderData.note
    }
    console.log('orderApi.createOrder payload:', payload)
    return api.post('/orders/from-cart', payload)
  },

  // 取消订单 - 对应后端 POST /orders/{orderId}/cancel
  cancelOrder(orderId) {
    return api.post(`/orders/${orderId}/cancel`)
  },

  // 支付订单 - 对应后端 POST /orders/{orderId}/pay
  payOrder(orderId) {
    return api.post(`/orders/${orderId}/pay`)
  },

  // 确认收货 - 对应后端 POST /orders/{orderId}/confirm
  confirmReceipt(orderId) {
    // 后端使用 POST 方法
    return api.post(`/orders/${orderId}/confirm`)
  },

  // 删除订单 - 对应后端 DELETE /orders/{orderId}
  deleteOrder(orderId) {
    return api.delete(`/orders/${orderId}`)
  },

  // 获取订单数量统计 - 对应后端 GET /orders/count?status=xxx
  getOrderCount(status) {
    return api.get('/orders/count', { params: { status } })
  }
}

// 辅助函数：格式化地址
function formatAddress(address) {
  if (!address) return ''
  const { province, city, district, detail } = address
  return [province, city, district, detail].filter(Boolean).join(' ')
}