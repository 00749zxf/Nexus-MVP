import api from './index'

export const favoriteApi = {
  // 获取所有收藏
  getFavorites() {
    return api.get('/favorites')
  },

  // 分页获取收藏
  getFavoritesPage(pageNum = 1, pageSize = 10) {
    return api.get('/favorites/page', { params: { pageNum, pageSize } })
  },

  // 添加收藏
  addFavorite(productId) {
    return api.post('/favorites', { productId })
  },

  // 删除收藏（按收藏ID）
  removeFavorite(favoriteId) {
    return api.delete(`/favorites/${favoriteId}`)
  },

  // 删除收藏（按商品ID）
  removeFavoriteByProductId(productId) {
    return api.delete(`/favorites/product/${productId}`)
  },

  // 检查是否已收藏
  checkFavorite(productId) {
    return api.get(`/favorites/check/${productId}`)
  },

  // 获取收藏数量
  getFavoriteCount() {
    return api.get('/favorites/count')
  },

  // 从购物车移入收藏
  moveFromCart(productId, cartItemId) {
    return api.post('/favorites/move-from-cart', null, {
      params: { productId, cartItemId }
    })
  }
}