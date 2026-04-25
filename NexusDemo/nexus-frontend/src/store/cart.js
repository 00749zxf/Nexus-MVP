import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { cartApi } from '@/api/cart'

export const useCartStore = defineStore('cart', () => {
  // 购物车商品列表
  const items = ref([])

  // 加载购物车数据
  const loadCart = async () => {
    try {
      const response = await cartApi.getCart()
      items.value = response.map(item => ({
        ...item,
        available: item.stock > 0,
        selected: item.selected || false,
        promotions: item.promotions || [],
        stock: Math.max(item.stock || 1, 1)
      }))
    } catch (error) {
      console.error('加载购物车失败:', error)
      // API失败时返回空数组，不使用模拟数据
      items.value = []
    }
  }

  // 添加商品到购物车
  const addToCart = async (product, quantity = 1, specs = []) => {
    try {
      const cartItem = {
        productId: product.id,
        quantity,
        specs
      }
      await cartApi.addToCart(cartItem)

      // 重新加载购物车数据
      await loadCart()

      ElMessage.success('已添加到购物车')
    } catch (error) {
      console.error('添加到购物车失败:', error)
      // 如果API失败，使用本地逻辑
      const existingItem = items.value.find(item =>
        item.productId === product.id &&
        JSON.stringify(item.specs) === JSON.stringify(specs)
      )

      if (existingItem) {
        existingItem.quantity += quantity
        if (existingItem.quantity > existingItem.stock) {
          existingItem.quantity = existingItem.stock
        }
        updateItemSubtotal(existingItem)
      } else {
        const newItem = {
          id: Date.now(),
          productId: product.id,
          name: product.name,
          image: product.image,
          specs,
          price: product.price,
          originalPrice: product.originalPrice,
          quantity,
          stock: product.stock || 100,
          available: true,
          selected: true,
          promotions: product.promotions || []
        }
        updateItemSubtotal(newItem)
        items.value.push(newItem)
        ElMessage.success('已添加到购物车')
      }
    }
  }

  // 更新商品数量
  const updateQuantity = async (itemId, quantity) => {
    try {
      await cartApi.updateCartItem(itemId, quantity)

      // 更新本地数据
      const item = items.value.find(item => item.id === itemId)
      if (item && item.available) {
        item.quantity = quantity
        updateItemSubtotal(item)
      }
    } catch (error) {
      console.error('更新数量失败:', error)

      // 检查是否是403错误（未授权）
      if (error.response && error.response.status === 403) {
        ElMessage.error('请先登录后再操作')
      } else {
        ElMessage.error('更新数量失败')
      }
    }
  }

  // 删除购物车商品
  const removeItem = async (itemId) => {
    try {
      await cartApi.removeCartItem(itemId)
      items.value = items.value.filter(item => item.id !== itemId)
      ElMessage.success('删除成功')
    } catch (error) {
      console.error('删除商品失败:', error)

      // 检查是否是403错误（未授权）
      if (error.response && error.response.status === 403) {
        ElMessage.error('请先登录后再操作')
      } else {
        ElMessage.error('删除商品失败')
      }
    }
  }

  // 清空购物车
  const clearCart = async () => {
    try {
      await cartApi.clearCart()
      items.value = []
      ElMessage.success('购物车已清空')
    } catch (error) {
      console.error('清空购物车失败:', error)

      // 检查是否是403错误（未授权）
      if (error.response && error.response.status === 403) {
        ElMessage.error('请先登录后再操作')
        // 可以重定向到登录页面
        // router.push('/login')
      } else {
        ElMessage.error('清空购物车失败')
      }
    }
  }

  // 更新商品选择状态
  const updateSelection = async (itemId, selected) => {
    try {
      await cartApi.updateItemSelection(itemId, selected)

      const item = items.value.find(item => item.id === itemId)
      if (item && item.available) {
        item.selected = selected
      }
    } catch (error) {
      console.error('更新选择状态失败:', error)

      // 检查是否是403错误（未授权）
      if (error.response && error.response.status === 403) {
        ElMessage.error('请先登录后再操作')
      } else {
        // 仍然更新本地状态
        const item = items.value.find(item => item.id === itemId)
        if (item && item.available) {
          item.selected = selected
        }
      }
    }
  }

  // 全选/取消全选
  const selectAll = (selected) => {
    items.value.forEach(item => {
      if (item.available) {
        item.selected = selected
      }
    })
  }

  // 计算商品小计
  const updateItemSubtotal = (item) => {
    item.subtotal = item.price * item.quantity
    item.saveAmount = item.originalPrice
      ? (item.originalPrice - item.price) * item.quantity
      : 0
  }

  // 计算选中的商品
  const selectedItems = computed(() => {
    return items.value.filter(item => item.selected && item.available)
  })

  // 计算选中商品数量
  const selectedCount = computed(() => {
    return selectedItems.value.reduce((total, item) => total + item.quantity, 0)
  })

  // 计算总金额
  const totalAmount = computed(() => {
    return selectedItems.value.reduce((total, item) => {
      return total + (item.price * item.quantity)
    }, 0)
  })

  // 计算节省金额
  const discountAmount = computed(() => {
    return selectedItems.value.reduce((total, item) => {
      const itemDiscount = item.originalPrice
        ? (item.originalPrice - item.price) * item.quantity
        : 0
      return total + itemDiscount
    }, 0)
  })

  // 计算是否全选
  const isAllSelected = computed(() => {
    const availableItems = items.value.filter(item => item.available)
    if (availableItems.length === 0) return false
    return availableItems.every(item => item.selected)
  })

  // 计算是否部分选择
  const isIndeterminate = computed(() => {
    const availableItems = items.value.filter(item => item.available)
    if (availableItems.length === 0) return false
    const selectedCount = availableItems.filter(item => item.selected).length
    return selectedCount > 0 && selectedCount < availableItems.length
  })

  return {
    items,
    loadCart,
    addToCart,
    updateQuantity,
    removeItem,
    clearCart,
    updateSelection,
    selectAll,
    selectedItems,
    selectedCount,
    totalAmount,
    discountAmount,
    isAllSelected,
    isIndeterminate
  }
})