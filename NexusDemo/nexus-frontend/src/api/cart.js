import api from './index'

export const cartApi = {
  // 获取购物车列表 - 对应后端 GET /cart
  getCart() {
    return api.get('/cart')
  },

  // 添加到购物车 - 对应后端 POST /cart
  addToCart(item) {
    return api.post('/cart', {
      productId: item.productId,
      quantity: item.quantity || 1,
      selected: item.selected || true
    })
  },

  // 更新购物车商品数量 - 对应后端 PUT /cart/{itemId}?quantity=xxx
  updateCartItem(itemId, quantity) {
    // 后端使用 @RequestParam，需要传递 query 参数
    return api.put(`/cart/${itemId}`, null, {
      params: { quantity }
    })
  },

  // 删除购物车商品 - 对应后端 DELETE /cart/{itemId}
  removeCartItem(itemId) {
    return api.delete(`/cart/${itemId}`)
  },

  // 清空购物车 - 对应后端 DELETE /cart/clear
  clearCart() {
    return api.delete('/cart/clear')
  },

  // 更新商品选择状态 - 对应后端 PUT /cart/{itemId}/selection?selected=xxx
  updateItemSelection(itemId, selected) {
    // 后端使用 @RequestParam，需要传递 query 参数
    return api.put(`/cart/${itemId}/selection`, null, {
      params: { selected }
    })
  },

  // 批量更新选择状态 - 对应后端 PUT /cart/batch-selection?itemIds=xxx&selected=xxx
  batchUpdateSelection(itemIds, selected) {
    // 后端使用 @RequestParam，需要传递 query 参数
    return api.put('/cart/batch-selection', null, {
      params: {
        itemIds: itemIds.join(','),
        selected
      }
    })
  }
}