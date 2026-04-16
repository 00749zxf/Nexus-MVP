import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { favoriteApi } from '@/api/favorite'

export const useFavoriteStore = defineStore('favorite', () => {
  // 状态
  const favorites = ref([])
  const favoriteCount = ref(0)
  const loading = ref(false)
  const favoriteStatusMap = ref({}) // 商品收藏状态缓存

  // 计算属性
  const hasFavorites = computed(() => favorites.value.length > 0)

  // 加载收藏列表
  const loadFavorites = async () => {
    loading.value = true
    try {
      favorites.value = await favoriteApi.getFavorites()
      favoriteCount.value = favorites.value.length
      // 更新收藏状态缓存
      favorites.value.forEach(f => {
        favoriteStatusMap.value[f.productId] = true
      })
    } catch (error) {
      console.error('加载收藏失败:', error)
      favorites.value = []
    } finally {
      loading.value = false
    }
  }

  // 添加收藏
  const addFavorite = async (productId) => {
    try {
      await favoriteApi.addFavorite(productId)
      favoriteStatusMap.value[productId] = true
      ElMessage.success('已添加到收藏')
      // 重新加载收藏列表（计数会在 loadFavorites 中更新）
      await loadFavorites()
    } catch (error) {
      ElMessage.error(error.message || '添加收藏失败')
      throw error
    }
  }

  // 删除收藏（按商品ID）
  const removeFavoriteByProductId = async (productId) => {
    try {
      await favoriteApi.removeFavoriteByProductId(productId)
      favoriteStatusMap.value[productId] = false
      ElMessage.success('已取消收藏')
      // 重新加载收藏列表（计数会在 loadFavorites 中更新）
      await loadFavorites()
    } catch (error) {
      ElMessage.error(error.message || '取消收藏失败')
      throw error
    }
  }

  // 删除收藏（按收藏ID）
  const removeFavorite = async (favoriteId) => {
    try {
      await favoriteApi.removeFavorite(favoriteId)
      ElMessage.success('已删除收藏')
      // 重新加载收藏列表（计数会在 loadFavorites 中更新）
      await loadFavorites()
    } catch (error) {
      ElMessage.error(error.message || '删除收藏失败')
      throw error
    }
  }

  // 检查是否已收藏
  const checkFavorite = async (productId) => {
    try {
      const isFavorite = await favoriteApi.checkFavorite(productId)
      favoriteStatusMap.value[productId] = isFavorite
      return isFavorite
    } catch (error) {
      console.error('检查收藏状态失败:', error)
      return false
    }
  }

  // 从购物车移入收藏
  const moveFromCart = async (productId, cartItemId) => {
    try {
      await favoriteApi.moveFromCart(productId, cartItemId)
      favoriteStatusMap.value[productId] = true
      ElMessage.success('已移入收藏')
      // 重新加载收藏列表（计数会在 loadFavorites 中更新）
      await loadFavorites()
    } catch (error) {
      ElMessage.error(error.message || '移入收藏失败')
      throw error
    }
  }

  // 判断商品是否已收藏（使用缓存）
  const isFavorited = (productId) => {
    return favoriteStatusMap.value[productId] === true
  }

  // 切换收藏状态
  const toggleFavorite = async (productId) => {
    if (isFavorited(productId)) {
      await removeFavoriteByProductId(productId)
    } else {
      await addFavorite(productId)
    }
  }

  // 获取收藏数量
  const getFavoriteCount = async () => {
    try {
      favoriteCount.value = await favoriteApi.getFavoriteCount()
      return favoriteCount.value
    } catch (error) {
      console.error('获取收藏数量失败:', error)
      return 0
    }
  }

  return {
    favorites,
    favoriteCount,
    loading,
    favoriteStatusMap,
    hasFavorites,
    loadFavorites,
    addFavorite,
    removeFavorite,
    removeFavoriteByProductId,
    checkFavorite,
    moveFromCart,
    isFavorited,
    toggleFavorite,
    getFavoriteCount
  }
})