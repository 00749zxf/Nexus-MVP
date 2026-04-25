<template>
  <div class="favorites-page">
    <!-- 面包屑导航 -->
    <div class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>我的收藏</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="favorites-content">
      <!-- 页面标题 -->
      <div class="page-header">
        <h1 class="page-title">
          <el-icon><Star /></el-icon>
          我的收藏
        </h1>
        <span class="favorite-count">共 {{ favoriteStore.favoriteCount }} 件商品</span>
      </div>

      <!-- 批量操作工具栏 -->
      <div v-if="favoriteStore.hasFavorites" class="batch-toolbar">
        <div class="batch-left">
          <el-checkbox v-model="selectAll" @change="handleSelectAll">
            全选
          </el-checkbox>
          <span class="selected-count">
            已选 {{ selectedItems.length }} 件
          </span>
        </div>
        <div class="batch-right">
          <el-button
            type="primary"
            size="small"
            :disabled="availableSelectedItems.length === 0"
            @click="handleBatchAddToCart"
          >
            <el-icon><ShoppingCart /></el-icon>
            批量加入购物车
          </el-button>
          <el-button
            type="danger"
            size="small"
            plain
            :disabled="selectedItems.length === 0"
            @click="handleBatchDelete"
          >
            <el-icon><Delete /></el-icon>
            批量删除
          </el-button>
        </div>
      </div>

      <!-- 收藏列表 -->
      <div v-if="favoriteStore.hasFavorites" class="favorites-list">
        <div
          v-for="item in favoriteStore.favorites"
          :key="item.id"
          class="favorite-item"
          :class="{ 'is-selected': item.selected }"
        >
          <!-- 复选框 -->
          <div class="item-checkbox">
            <el-checkbox
              v-model="item.selected"
              :disabled="!item.productAvailable || item.productStock <= 0"
              @change="updateSelectAllStatus"
            />
          </div>

          <div class="item-image" @click="goToProduct(item.productId)">
            <img :src="item.productImage" :alt="item.productName" />
            <div v-if="!item.productAvailable" class="sold-out-overlay">
              已下架
            </div>
          </div>

          <div class="item-info" @click="goToProduct(item.productId)">
            <h3 class="item-name">{{ item.productName }}</h3>
            <div class="item-price">
              <span class="price">¥{{ item.productPrice }}</span>
              <span v-if="item.productStock > 0" class="stock">库存: {{ item.productStock }}</span>
              <span v-else class="stock out-of-stock">缺货</span>
            </div>
          </div>

          <div class="item-actions">
            <el-button
              type="primary"
              size="small"
              :disabled="!item.productAvailable || item.productStock <= 0"
              @click="addToCart(item)"
            >
              <el-icon><ShoppingCart /></el-icon>
              加入购物车
            </el-button>

            <el-button
              type="danger"
              size="small"
              plain
              @click="removeFavorite(item.id)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="empty-state">
        <el-empty description="暂无收藏商品">
          <template #image>
            <el-icon :size="60" class="empty-icon"><Star /></el-icon>
          </template>
          <el-button type="primary" @click="$router.push('/products')">
            去逛逛
          </el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, ShoppingCart, Delete } from '@element-plus/icons-vue'
import { useFavoriteStore } from '@/store/favorite'
import { useCartStore } from '@/store/cart'

const router = useRouter()
const favoriteStore = useFavoriteStore()
const cartStore = useCartStore()

// 全选状态
const selectAll = ref(false)

// 已选中的收藏项
const selectedItems = computed(() => {
  return favoriteStore.favorites.filter(item => item.selected)
})

// 可操作的已选项（有库存且未下架）
const availableSelectedItems = computed(() => {
  return selectedItems.value.filter(item =>
    item.productAvailable && item.productStock > 0
  )
})

// 全选/取消全选
const handleSelectAll = (value) => {
  favoriteStore.favorites.forEach(item => {
    // 只选择可操作的商品
    if (item.productAvailable && item.productStock > 0) {
      item.selected = value
    } else {
      item.selected = false
    }
  })
}

// 更新全选状态
const updateSelectAllStatus = () => {
  const availableItems = favoriteStore.favorites.filter(item =>
    item.productAvailable && item.productStock > 0
  )
  selectAll.value = availableItems.length > 0 &&
    availableItems.every(item => item.selected)
}

// 跳转到商品详情
const goToProduct = (productId) => {
  router.push(`/products/${productId}`)
}

// 加入购物车
const addToCart = async (item) => {
  try {
    await cartStore.addToCart({
      id: item.productId,
      name: item.productName,
      price: item.productPrice,
      image: item.productImage,
      stock: item.productStock
    }, 1)
    ElMessage.success('已加入购物车')
  } catch (error) {
    ElMessage.error('加入购物车失败')
  }
}

// 删除收藏
const removeFavorite = async (favoriteId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个收藏吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await favoriteStore.removeFavorite(favoriteId)
  } catch {
    // 用户取消
  }
}

// 批量加入购物车
const handleBatchAddToCart = async () => {
  const items = availableSelectedItems.value
  if (items.length === 0) {
    ElMessage.warning('请选择可购买的商品')
    return
  }

  try {
    for (const item of items) {
      await cartStore.addToCart({
        id: item.productId,
        name: item.productName,
        price: item.productPrice,
        image: item.productImage,
        stock: item.productStock
      }, 1)
    }
    ElMessage.success(`已将 ${items.length} 件商品加入购物车`)
    // 清除选中状态
    favoriteStore.favorites.forEach(item => item.selected = false)
    selectAll.value = false
  } catch (error) {
    ElMessage.error('批量加入购物车失败')
  }
}

// 批量删除
const handleBatchDelete = async () => {
  const items = selectedItems.value
  if (items.length === 0) {
    ElMessage.warning('请选择要删除的商品')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${items.length} 件收藏吗？`,
      '批量删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 逐个删除
    for (const item of items) {
      await favoriteStore.removeFavorite(item.id)
    }

    selectAll.value = false
  } catch {
    // 用户取消
  }
}

// 初始化
onMounted(async () => {
  await favoriteStore.loadFavorites()
  // 为每个收藏项添加 selected 属性
  favoriteStore.favorites.forEach(item => {
    item.selected = false
  })
})
</script>

<style scoped>
.favorites-page {
  padding-bottom: var(--space-xl);
}

.breadcrumb {
  margin-bottom: var(--space-lg);
}

.favorites-content {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  padding: var(--space-xl);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--border-color-light);
}

.page-title {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-xl);
  color: var(--text-primary);
  margin: 0;
}

.page-title .el-icon {
  color: var(--warning-color);
}

.favorite-count {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

/* 批量操作工具栏 */
.batch-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-lg);
  padding: var(--space-md);
  background-color: var(--bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
}

.batch-left {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.selected-count {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.batch-right {
  display: flex;
  gap: var(--space-sm);
}

.favorites-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-lg);
}

.favorite-item {
  background-color: var(--bg-color);
  border-radius: var(--border-radius);
  overflow: hidden;
  transition: transform 0.3s, box-shadow 0.3s, border-color 0.3s;
  border: 2px solid var(--border-color-light);
  position: relative;
}

.favorite-item:hover {
  transform: translateY(-4px);
  box-shadow: var(--box-shadow);
}

.favorite-item.is-selected {
  border-color: var(--primary-color);
}

/* 复选框 */
.item-checkbox {
  position: absolute;
  top: var(--space-sm);
  left: var(--space-sm);
  z-index: 10;
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  padding: 4px;
}

.item-image {
  position: relative;
  width: 100%;
  height: 200px;
  overflow: hidden;
  cursor: pointer;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.item-image:hover img {
  transform: scale(1.05);
}

.sold-out-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: var(--font-size-lg);
  font-weight: 500;
}

.item-info {
  padding: var(--space-md);
  cursor: pointer;
}

.item-name {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin: 0 0 var(--space-sm) 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-price {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.price {
  font-size: var(--font-size-lg);
  color: var(--danger-color);
  font-weight: 600;
}

.stock {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.stock.out-of-stock {
  color: var(--danger-color);
}

.item-actions {
  display: flex;
  gap: var(--space-sm);
  padding: var(--space-md);
  padding-top: 0;
}

.item-actions .el-button {
  flex: 1;
}

.empty-state {
  padding: var(--space-xl) 0;
}

.empty-icon {
  color: var(--text-secondary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .batch-toolbar {
    flex-direction: column;
    gap: var(--space-md);
  }

  .batch-left,
  .batch-right {
    width: 100%;
    justify-content: space-between;
  }

  .favorites-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .favorites-list {
    grid-template-columns: 1fr;
  }
}
</style>