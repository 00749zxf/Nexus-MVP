<template>
  <div class="product-list-page">
    <!-- 面包屑导航 -->
    <div class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>商品列表</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="page-content">
      <!-- 侧边筛选栏 -->
      <div class="sidebar">
        <!-- 分类筛选 -->
        <div class="filter-section">
          <h3 class="filter-title">商品分类</h3>
          <el-tree
            :data="categoryTree"
            node-key="id"
            default-expand-all
            :expand-on-click-node="false"
            @node-click="handleCategoryClick"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <span>{{ node.label }}</span>
                <span class="category-count">({{ data.count || 0 }})</span>
              </span>
            </template>
          </el-tree>
        </div>

        <!-- 价格筛选 -->
        <div class="filter-section">
          <h3 class="filter-title">价格区间</h3>
          <div class="price-filter">
            <el-slider
              v-model="priceRange"
              range
              :min="0"
              :max="10000"
              :step="100"
              @change="handlePriceChange"
            />
            <div class="price-inputs">
              <el-input-number
                v-model="priceRange[0]"
                :min="0"
                :max="priceRange[1]"
                :step="100"
                controls-position="right"
                size="small"
              />
              <span class="separator">-</span>
              <el-input-number
                v-model="priceRange[1]"
                :min="priceRange[0]"
                :max="10000"
                :step="100"
                controls-position="right"
                size="small"
              />
            </div>
          </div>
        </div>

        <!-- 品牌筛选 -->
        <div class="filter-section">
          <h3 class="filter-title">品牌</h3>
          <el-checkbox-group v-model="selectedBrands" @change="handleBrandChange">
            <div class="brand-list">
              <el-checkbox
                v-for="brand in brands"
                :key="brand.id"
                :label="brand.id"
                class="brand-item"
              >
                {{ brand.name }}
                <span class="brand-count">({{ brand.count }})</span>
              </el-checkbox>
            </div>
          </el-checkbox-group>
        </div>

        <!-- 筛选器 -->
        <div class="filter-section">
          <h3 class="filter-title">其他筛选</h3>
          <div class="other-filters">
            <div class="filter-item">
              <span class="filter-label">仅看有货</span>
              <el-switch v-model="inStockOnly" @change="handleStockChange" />
            </div>
            <div class="filter-item">
              <span class="filter-label">仅看新品</span>
              <el-switch v-model="newOnly" @change="handleNewChange" />
            </div>
            <div class="filter-item">
              <span class="filter-label">仅看促销</span>
              <el-switch v-model="onSaleOnly" @change="handleSaleChange" />
            </div>
          </div>
        </div>

        <!-- 重置筛选 -->
        <div class="filter-actions">
          <el-button type="primary" @click="resetFilters">重置筛选</el-button>
        </div>
      </div>

      <!-- 主内容区 -->
      <div class="main-content">
        <!-- 搜索和排序 -->
        <div class="toolbar">
          <div class="search-box">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品..."
              clearable
              @clear="handleSearchClear"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>
          </div>
          <div class="sort-options">
            <el-select v-model="sortBy" placeholder="排序方式" @change="handleSortChange">
              <el-option label="默认排序" value="default" />
              <el-option label="价格从低到高" value="price_asc" />
              <el-option label="价格从高到低" value="price_desc" />
              <el-option label="销量最高" value="sales_desc" />
              <el-option label="评价最好" value="rating_desc" />
              <el-option label="最新上架" value="newest" />
            </el-select>
          </div>
        </div>

        <!-- 商品网格 -->
        <div class="product-grid">
          <div
            v-for="product in products"
            :key="product.id"
            class="product-item"
            @click="$router.push(`/products/${product.id}`)"
          >
            <div class="product-image">
              <img :src="product.image" :alt="product.name" />
              <div class="product-tags">
                <span v-if="product.isNew" class="tag new">新品</span>
                <span v-if="product.isHot" class="tag hot">热卖</span>
                <span v-if="product.discount" class="tag discount">{{ product.discount }}折</span>
              </div>
              <div class="product-actions">
                <el-button
                  type="primary"
                  size="small"
                  :icon="ShoppingCart"
                  circle
                  @click.stop="addToCart(product)"
                />
                <el-button
                  type="info"
                  size="small"
                  :icon="Star"
                  circle
                  @click.stop="addToWishlist(product)"
                />
              </div>
            </div>
            <div class="product-info">
              <h3 class="product-name">{{ product.name }}</h3>
              <p class="product-desc">{{ product.description }}</p>
              <div class="product-meta">
                <span class="sales">销量: {{ product.sales }}</span>
                <span class="rating">
                  <el-rate
                    v-model="product.rating"
                    disabled
                    show-score
                    text-color="#ff9900"
                    score-template="{value}"
                  />
                </span>
              </div>
              <div class="product-price">
                <span class="current-price">¥{{ product.price }}</span>
                <span v-if="product.originalPrice" class="original-price">
                  ¥{{ product.originalPrice }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[12, 24, 36, 48]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, ShoppingCart, Star } from '@element-plus/icons-vue'
import { useProductStore } from '@/store/product'
import { useCartStore } from '@/store/cart'
import { useFavoriteStore } from '@/store/favorite'
import { useUserStore } from '@/store/user'

// 路由
const route = useRoute()
const router = useRouter()

// Store
const productStore = useProductStore()
const cartStore = useCartStore()
const favoriteStore = useFavoriteStore()
const userStore = useUserStore()

// 筛选状态
const categoryTree = ref([
  {
    id: 1,
    label: '家用电器',
    count: 120,
    children: [
      { id: 11, label: '电视', count: 40 },
      { id: 12, label: '冰箱', count: 35 },
      { id: 13, label: '空调', count: 45 }
    ]
  },
  {
    id: 2,
    label: '数码产品',
    count: 200,
    children: [
      { id: 21, label: '手机', count: 80 },
      { id: 22, label: '电脑', count: 70 },
      { id: 23, label: '相机', count: 50 }
    ]
  }
])

const priceRange = ref([0, 10000])
const selectedBrands = ref([])
const inStockOnly = ref(false)
const newOnly = ref(false)
const onSaleOnly = ref(false)
const searchKeyword = ref('')
const sortBy = ref('default')

// 品牌数据
const brands = ref([
  { id: 1, name: '华为', count: 45 },
  { id: 2, name: '小米', count: 38 },
  { id: 3, name: '苹果', count: 52 },
  { id: 4, name: '三星', count: 28 },
  { id: 5, name: '索尼', count: 21 }
])

// 分页状态
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

// 使用store中的商品数据
const products = computed(() => productStore.products)

// 方法
const handleCategoryClick = (data) => {
  console.log('选择分类:', data)
  loadProducts()
}

const handlePriceChange = () => {
  console.log('价格区间:', priceRange.value)
  loadProducts()
}

const handleBrandChange = () => {
  console.log('选择品牌:', selectedBrands.value)
  loadProducts()
}

const handleStockChange = () => {
  loadProducts()
}

const handleNewChange = () => {
  loadProducts()
}

const handleSaleChange = () => {
  loadProducts()
}

const handleSearch = () => {
  console.log('搜索关键词:', searchKeyword.value)
  loadProducts()
}

const handleSearchClear = () => {
  searchKeyword.value = ''
  loadProducts()
}

const handleSortChange = () => {
  console.log('排序方式:', sortBy.value)
  loadProducts()
}

const resetFilters = () => {
  priceRange.value = [0, 10000]
  selectedBrands.value = []
  inStockOnly.value = false
  newOnly.value = false
  onSaleOnly.value = false
  searchKeyword.value = ''
  sortBy.value = 'default'
  currentPage.value = 1

  loadProducts()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadProducts()
}

const handlePageChange = (page) => {
  currentPage.value = page
  loadProducts()
}

const addToCart = async (product) => {
  try {
    await cartStore.addToCart(product, 1, [])
    ElMessage.success(`已添加 ${product.name} 到购物车`)
  } catch (error) {
    console.error('添加到购物车失败:', error)
  }
}

const addToWishlist = async (product) => {
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    await favoriteStore.addFavorite(product.id)
  } catch (error) {
    console.error('添加收藏失败:', error)
  }
}

const loadProducts = async () => {
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value,
      sort: sortBy.value,
      minPrice: priceRange.value[0],
      maxPrice: priceRange.value[1],
      brands: selectedBrands.value.join(','),
      inStockOnly: inStockOnly.value,
      newOnly: newOnly.value,
      onSaleOnly: onSaleOnly.value
    }

    // 移除未定义的参数
    Object.keys(params).forEach(key => {
      if (params[key] === undefined || params[key] === null || params[key] === '') {
        delete params[key]
      }
    })

    await productStore.loadProducts(params)
    total.value = productStore.products.length
  } catch (error) {
    console.error('加载商品数据失败:', error)
  }
}

onMounted(() => {
  // 从路由参数初始化
  if (route.query.keyword) {
    searchKeyword.value = route.query.keyword
  }
  if (route.query.category) {
    // TODO: 设置选中分类
  }

  loadProducts()
})
</script>

<style scoped>
.product-list-page {
  padding-bottom: var(--space-xl);
}

.breadcrumb {
  margin-bottom: var(--space-lg);
}

.page-content {
  display: flex;
  gap: var(--space-lg);
}

/* 侧边栏 */
.sidebar {
  width: 250px;
  flex-shrink: 0;
}

.filter-section {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  padding: var(--space-md);
  margin-bottom: var(--space-md);
  border: 1px solid var(--border-color-light);
}

.filter-title {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin-bottom: var(--space-md);
  padding-bottom: var(--space-xs);
  border-bottom: 1px solid var(--border-color-light);
}

.tree-node {
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.category-count {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.price-filter {
  padding: 0 var(--space-sm);
}

.price-inputs {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-top: var(--space-md);
}

.separator {
  color: var(--text-secondary);
}

.brand-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.brand-item {
  width: 100%;
}

.brand-count {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.other-filters {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.filter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-label {
  font-size: var(--font-size-sm);
  color: var(--text-regular);
}

.filter-actions {
  text-align: center;
  margin-top: var(--space-lg);
}

/* 主内容区 */
.main-content {
  flex: 1;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-lg);
  background-color: var(--card-bg-color);
  padding: var(--space-md);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
}

.search-box {
  flex: 1;
  max-width: 400px;
}

.sort-options {
  width: 200px;
}

/* 商品网格 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: var(--space-lg);
  margin-bottom: var(--space-lg);
}

.product-item {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  overflow: hidden;
  cursor: pointer;
  transition: var(--transition-all);
}

.product-item:hover {
  transform: translateY(-4px);
  box-shadow: var(--box-shadow);
}

.product-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-tags {
  position: absolute;
  top: var(--space-sm);
  left: var(--space-sm);
  display: flex;
  gap: var(--space-xs);
}

.tag {
  padding: 2px 8px;
  border-radius: var(--border-radius);
  font-size: var(--font-size-xs);
  color: white;
  font-weight: bold;
}

.tag.new {
  background-color: var(--success-color);
}

.tag.hot {
  background-color: var(--danger-color);
}

.tag.discount {
  background-color: var(--warning-color);
}

.product-actions {
  position: absolute;
  top: var(--space-sm);
  right: var(--space-sm);
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  opacity: 0;
  transition: opacity 0.3s;
}

.product-item:hover .product-actions {
  opacity: 1;
}

.product-info {
  padding: var(--space-md);
}

.product-name {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desc {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-md);
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-md);
  font-size: var(--font-size-sm);
}

.sales {
  color: var(--text-secondary);
}

.rating {
  display: flex;
  align-items: center;
}

.product-price {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.current-price {
  font-size: var(--font-size-lg);
  color: var(--danger-color);
  font-weight: bold;
}

.original-price {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  text-decoration: line-through;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  background-color: var(--card-bg-color);
  padding: var(--space-md);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
}

/* 响应式设计 */
@media (max-width: 992px) {
  .page-content {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    gap: var(--space-md);
  }

  .search-box {
    max-width: 100%;
  }

  .sort-options {
    width: 100%;
  }

  .product-grid {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }
}
</style>