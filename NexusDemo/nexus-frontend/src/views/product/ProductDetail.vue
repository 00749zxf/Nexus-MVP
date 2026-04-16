<template>
  <div class="product-detail-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- 商品详情 -->
    <div v-else-if="product">
      <!-- 面包屑导航 -->
      <div class="breadcrumb">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/products' }">商品列表</el-breadcrumb-item>
          <el-breadcrumb-item>{{ product.name }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div class="product-detail">
        <!-- 商品图片 -->
        <div class="product-gallery">
          <div class="main-image">
            <img :src="currentImage" :alt="product.name" />
          </div>
          <div class="thumbnail-list" v-if="product.images?.length">
            <div
              v-for="(img, index) in product.images"
              :key="index"
              class="thumbnail"
              :class="{ active: currentImageIndex === index }"
              @click="currentImageIndex = index"
            >
              <img :src="img" :alt="`商品图片${index + 1}`" />
            </div>
          </div>
        </div>

        <!-- 商品信息 -->
        <div class="product-info">
          <h1 class="product-title">{{ product.name }}</h1>
          <div class="product-meta">
            <div class="meta-item" v-if="product.sku">
              <span class="label">商品编号:</span>
              <span class="value">{{ product.sku }}</span>
            </div>
            <div class="meta-item" v-if="product.brand">
              <span class="label">品牌:</span>
              <span class="value">{{ product.brand }}</span>
            </div>
            <div class="meta-item" v-if="product.category">
              <span class="label">分类:</span>
              <span class="value">{{ product.category }}</span>
            </div>
          </div>

          <!-- 价格信息 -->
          <div class="price-section">
            <div class="current-price">
              <span class="price">¥{{ product.price }}</span>
              <span v-if="product.discount" class="discount-rate">{{ product.discount }}折</span>
            </div>
            <div v-if="product.originalPrice" class="original-price">
              <span class="label">原价:</span>
              <span class="price">¥{{ product.originalPrice }}</span>
            </div>
            <div class="save-price" v-if="product.originalPrice">
              节省: ¥{{ (product.originalPrice - product.price).toFixed(2) }}
            </div>
          </div>

          <!-- 规格选择 -->
          <div class="spec-section" v-if="product.specs?.length">
            <div class="spec-item" v-for="spec in product.specs" :key="spec.name">
              <h3 class="spec-title">{{ spec.name }}:</h3>
              <div class="spec-options">
                <el-radio-group v-model="selectedSpecs[spec.name]">
                  <el-radio
                    v-for="option in spec.options"
                    :key="option.value"
                    :label="option.value"
                    :disabled="option.stock === 0"
                  >
                    {{ option.label }}
                    <span v-if="option.stock !== undefined" class="stock-info">
                      (库存: {{ option.stock }})
                    </span>
                  </el-radio>
                </el-radio-group>
              </div>
            </div>
          </div>

          <!-- 数量选择 -->
          <div class="quantity-section">
            <h3 class="section-title">数量:</h3>
            <div class="quantity-control">
              <el-input-number
                v-model="quantity"
                :min="1"
                :max="maxQuantity"
                controls-position="right"
                size="large"
              />
              <span class="stock-info">库存: {{ product.stock || 0 }}件</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="action-section">
            <el-button
              type="primary"
              size="large"
              :icon="ShoppingCart"
              @click="addToCart"
              :loading="addingToCart"
            >
              加入购物车
            </el-button>
            <el-button
              type="danger"
              size="large"
              :icon="ShoppingCart"
              @click="buyNow"
              :loading="buying"
            >
              立即购买
            </el-button>
            <el-button
              :type="isFavorited ? 'warning' : 'info'"
              size="large"
              :icon="Star"
              @click="addToWishlist"
            >
              {{ isFavorited ? '已收藏' : '收藏' }}
            </el-button>
          </div>

          <!-- 促销信息 -->
          <div class="promotion-section" v-if="product.promotions?.length > 0">
            <h3 class="section-title">促销活动:</h3>
            <ul class="promotion-list">
              <li v-for="promotion in product.promotions" :key="promotion.id">
                <el-tag :type="promotion.type">{{ promotion.text }}</el-tag>
              </li>
            </ul>
          </div>
        </div>
      </div>

    <!-- 商品详情标签页 -->
      <div class="product-tabs">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="商品详情" name="detail">
            <div class="detail-content" v-html="product.descriptionDetail || '暂无详情'"></div>
          </el-tab-pane>
          <el-tab-pane label="规格参数" name="spec" v-if="product.specTable?.length">
            <div class="spec-content">
              <el-table :data="product.specTable" border>
                <el-table-column prop="name" label="参数名" width="150" />
                <el-table-column prop="value" label="参数值" />
              </el-table>
            </div>
          </el-tab-pane>
          <el-tab-pane label="用户评价" name="review" v-if="product.rating">
            <div class="review-content">
              <div class="review-summary">
                <div class="rating-overview">
                  <div class="average-rating">
                    <span class="score">{{ product.rating.average || 0 }}</span>
                    <el-rate :model-value="product.rating.average || 0" disabled />
                    <span class="count">{{ product.rating.count || 0 }}人评价</span>
                  </div>
                  <div class="rating-distribution" v-if="product.rating.distribution">
                    <div
                      v-for="item in product.rating.distribution"
                      :key="item.stars"
                      class="distribution-item"
                    >
                      <span class="stars">{{ item.stars }}星</span>
                      <el-progress
                        :percentage="item.percentage"
                        :show-text="false"
                      />
                      <span class="percentage">{{ item.percentage }}%</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="review-list" v-if="product.reviews?.length">
                <div
                  v-for="review in product.reviews"
                  :key="review.id"
                  class="review-item"
                >
                  <div class="review-header">
                    <el-avatar :size="40" :src="review.avatar" />
                    <div class="reviewer-info">
                      <span class="username">{{ review.username }}</span>
                      <el-rate :model-value="review.rating" disabled size="small" />
                    </div>
                    <span class="review-time">{{ review.time }}</span>
                  </div>
                  <div class="review-content">
                    <p>{{ review.content }}</p>
                    <div class="review-images" v-if="review.images?.length > 0">
                      <img
                        v-for="(img, index) in review.images"
                        :key="index"
                        :src="img"
                        class="review-image"
                      />
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="no-reviews">
                <el-empty description="暂无评价" />
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="常见问题" name="faq" v-if="product.faqs?.length">
            <div class="faq-content">
              <el-collapse v-model="activeFaq">
                <el-collapse-item
                  v-for="faq in product.faqs"
                  :key="faq.id"
                  :title="faq.question"
                  :name="faq.id"
                >
                  <div v-html="faq.answer"></div>
                </el-collapse-item>
              </el-collapse>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 推荐商品 -->
      <div class="related-products" v-if="relatedProducts.length > 0">
        <h2 class="section-title">相关推荐</h2>
        <div class="related-list">
          <div
            v-for="item in relatedProducts"
            :key="item.id"
            class="related-item"
            @click="$router.push(`/products/${item.id}`)"
          >
            <img :src="item.image" :alt="item.name" />
            <h3>{{ item.name }}</h3>
            <div class="price">¥{{ item.price }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else class="error-state">
      <el-empty description="商品不存在或已下架">
        <el-button type="primary" @click="$router.push('/products')">返回商品列表</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ShoppingCart, Star } from '@element-plus/icons-vue'
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

// 状态
const loading = ref(true)
const currentImageIndex = ref(0)
const selectedSpecs = reactive({})
const quantity = ref(1)
const activeTab = ref('detail')
const activeFaq = ref([])
const addingToCart = ref(false)
const buying = ref(false)
const isFavorited = ref(false)

// 商品数据 - 使用store中的currentProduct
const product = computed(() => productStore.currentProduct)

// 相关商品
const relatedProducts = ref([])

// 计算属性
const currentImage = computed(() => {
  return product.value?.images?.[currentImageIndex.value] || product.value?.image || '/placeholder.png'
})

const maxQuantity = computed(() => {
  return product.value?.stock || 99
})

// 方法
const loadProduct = async () => {
  loading.value = true
  try {
    const productId = route.params.id
    await productStore.getProductById(productId)

    // 初始化规格选择
    if (product.value?.specifications) {
      product.value.specifications.forEach(spec => {
        const firstAvailableOption = spec.options?.find(opt => opt.stock > 0)
        if (firstAvailableOption) {
          selectedSpecs[spec.name] = firstAvailableOption.value
        }
      })
    }
  } catch (error) {
    console.error('加载商品详情失败:', error)
    ElMessage.error('加载商品详情失败')
  } finally {
    loading.value = false
  }
}

const addToCart = async () => {
  if (!product.value) return
  addingToCart.value = true
  try {
    await cartStore.addToCart(product.value, quantity.value)
  } catch (error) {
    console.error('添加到购物车失败:', error)
    ElMessage.error('添加失败')
  } finally {
    addingToCart.value = false
  }
}

const buyNow = async () => {
  if (!product.value) return
  buying.value = true
  try {
    // 直接购买：先加入购物车，然后跳转到结算页面
    await cartStore.addToCart(product.value, quantity.value)
    // 跳转到结算页面
    router.push('/checkout')
  } catch (error) {
    ElMessage.error('购买失败')
  } finally {
    buying.value = false
  }
}

const addToWishlist = async () => {
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  if (!product.value) return

  try {
    if (isFavorited.value) {
      await favoriteStore.removeFavoriteByProductId(product.value.id)
      isFavorited.value = false
    } else {
      await favoriteStore.addFavorite(product.value.id)
      isFavorited.value = true
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
  }
}

// 检查收藏状态
const checkFavoriteStatus = async () => {
  if (!userStore.isAuthenticated || !product.value) return
  try {
    isFavorited.value = await favoriteStore.checkFavorite(product.value.id)
  } catch (error) {
    console.error('检查收藏状态失败:', error)
  }
}

onMounted(async () => {
  await loadProduct()
  await checkFavoriteStatus()
})
</script>

<style scoped>
.product-detail-page {
  padding-bottom: var(--space-xl);
}

.loading-state {
  padding: var(--space-xl);
}

.error-state {
  padding: var(--space-xl);
  text-align: center;
}

.breadcrumb {
  margin-bottom: var(--space-lg);
}

/* 商品详情区域 */
.product-detail {
  display: flex;
  gap: var(--space-xl);
  margin-bottom: var(--space-xl);
  background-color: var(--card-bg-color);
  padding: var(--space-xl);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
}

/* 商品图片 */
.product-gallery {
  flex: 1;
  max-width: 500px;
}

.main-image {
  width: 100%;
  height: 400px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  overflow: hidden;
  margin-bottom: var(--space-md);
}

.main-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.thumbnail-list {
  display: flex;
  gap: var(--space-sm);
  overflow-x: auto;
  padding-bottom: var(--space-xs);
}

.thumbnail {
  width: 80px;
  height: 80px;
  border: 2px solid var(--border-color);
  border-radius: var(--border-radius);
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
}

.thumbnail.active {
  border-color: var(--primary-color);
}

.thumbnail img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 商品信息 */
.product-info {
  flex: 1;
  max-width: 600px;
}

.product-title {
  font-size: var(--font-size-xl);
  color: var(--text-primary);
  margin-bottom: var(--space-md);
}

.product-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-lg);
  margin-bottom: var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: 1px solid var(--border-color-light);
}

.meta-item {
  display: flex;
  gap: var(--space-xs);
}

.meta-item .label {
  color: var(--text-secondary);
}

.meta-item .value {
  color: var(--text-primary);
  font-weight: 500;
}

/* 价格区域 */
.price-section {
  margin-bottom: var(--space-lg);
  padding: var(--space-md);
  background-color: var(--bg-color);
  border-radius: var(--border-radius);
}

.current-price {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-xs);
}

.current-price .price {
  font-size: 32px;
  color: var(--danger-color);
  font-weight: bold;
}

.discount-rate {
  padding: 4px 8px;
  background-color: var(--danger-color);
  color: white;
  border-radius: var(--border-radius);
  font-size: var(--font-size-sm);
  font-weight: bold;
}

.original-price {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-xs);
}

.original-price .label {
  color: var(--text-secondary);
}

.original-price .price {
  text-decoration: line-through;
  color: var(--text-secondary);
}

.save-price {
  color: var(--success-color);
  font-weight: 500;
}

/* 规格选择 */
.spec-section {
  margin-bottom: var(--space-lg);
}

.spec-item {
  margin-bottom: var(--space-md);
}

.spec-title {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin-bottom: var(--space-sm);
}

.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.stock-info {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

/* 数量选择 */
.quantity-section {
  margin-bottom: var(--space-lg);
}

.section-title {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin-bottom: var(--space-sm);
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}

/* 操作按钮 */
.action-section {
  display: flex;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

/* 促销信息 */
.promotion-section {
  margin-bottom: var(--space-lg);
}

.promotion-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 商品标签页 */
.product-tabs {
  background-color: var(--card-bg-color);
  padding: var(--space-xl);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  margin-bottom: var(--space-xl);
}

.detail-content {
  line-height: 1.6;
}

.detail-content h3 {
  color: var(--text-primary);
  margin: var(--space-lg) 0 var(--space-md);
}

.detail-content ul {
  padding-left: var(--space-lg);
  margin-bottom: var(--space-md);
}

.detail-content li {
  margin-bottom: var(--space-xs);
  color: var(--text-regular);
}

/* 评价内容 */
.review-summary {
  background-color: var(--bg-color);
  padding: var(--space-lg);
  border-radius: var(--border-radius);
  margin-bottom: var(--space-lg);
}

.rating-overview {
  display: flex;
  gap: var(--space-xl);
}

.average-rating {
  text-align: center;
  min-width: 150px;
}

.average-rating .score {
  font-size: 48px;
  font-weight: bold;
  color: var(--danger-color);
  display: block;
  line-height: 1;
}

.average-rating .count {
  display: block;
  color: var(--text-secondary);
  margin-top: var(--space-xs);
}

.rating-distribution {
  flex: 1;
}

.distribution-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
}

.distribution-item .stars {
  width: 50px;
  color: var(--text-secondary);
}

.distribution-item .percentage {
  width: 40px;
  text-align: right;
  color: var(--text-secondary);
}

.review-item {
  padding: var(--space-lg) 0;
  border-bottom: 1px solid var(--border-color-light);
}

.review-item:last-child {
  border-bottom: none;
}

.review-header {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}

.reviewer-info {
  flex: 1;
}

.reviewer-info .username {
  display: block;
  font-weight: 500;
  margin-bottom: var(--space-xs);
}

.review-time {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.review-content p {
  line-height: 1.6;
  margin-bottom: var(--space-md);
}

.review-images {
  display: flex;
  gap: var(--space-sm);
}

.review-image {
  width: 100px;
  height: 100px;
  border-radius: var(--border-radius);
  object-fit: cover;
}

/* 常见问题 */
.faq-content {
  padding: var(--space-md);
}

/* 相关商品 */
.related-products {
  background-color: var(--card-bg-color);
  padding: var(--space-xl);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
}

.related-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-lg);
  margin-top: var(--space-lg);
}

.related-item {
  text-align: center;
  cursor: pointer;
  transition: var(--transition-all);
}

.related-item:hover {
  transform: translateY(-4px);
}

.related-item img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: var(--border-radius);
  margin-bottom: var(--space-sm);
}

.related-item h3 {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.related-item .price {
  color: var(--danger-color);
  font-weight: bold;
}

/* 响应式设计 */
@media (max-width: 992px) {
  .product-detail {
    flex-direction: column;
  }

  .product-gallery {
    max-width: 100%;
  }

  .rating-overview {
    flex-direction: column;
    gap: var(--space-lg);
  }
}

@media (max-width: 768px) {
  .action-section {
    flex-direction: column;
  }

  .action-section .el-button {
    width: 100%;
  }

  .related-list {
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  }
}
</style>