<template>
  <div class="home-page">
    <!-- 轮播图 -->
    <div class="banner-section">
      <el-carousel height="400px" arrow="always" indicator-position="outside">
        <el-carousel-item v-for="item in banners" :key="item.id">
          <div class="banner-item" :style="{ backgroundImage: `url(${item.image})` }">
            <div class="banner-content">
              <h2>{{ item.title }}</h2>
              <p>{{ item.description }}</p>
              <el-button type="primary" size="large">
                立即购买
              </el-button>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- 分类导航 -->
    <div class="category-section">
      <h2 class="section-title">商品分类</h2>
      <div class="category-list">
        <div
          v-for="category in categories"
          :key="category.id"
          class="category-item"
          @click="$router.push(`/products?category=${category.id}`)"
        >
          <div class="category-icon">
            <el-icon :size="40">
              <component :is="category.icon" />
            </el-icon>
          </div>
          <span class="category-name">{{ category.name }}</span>
        </div>
      </div>
    </div>

    <!-- 推荐商品 -->
    <div class="featured-section">
      <div class="section-header">
        <h2 class="section-title">热门推荐</h2>
        <router-link to="/products" class="view-all">
          查看全部
          <el-icon><ArrowRight /></el-icon>
        </router-link>
      </div>
      <div class="product-list">
        <div
          v-for="product in featuredProducts"
          :key="product.id"
          class="product-card"
          @click="$router.push(`/products/${product.id}`)"
        >
          <div class="product-image">
            <img :src="product.image" :alt="product.name" />
            <div class="product-tags">
              <span v-if="product.isNew" class="tag new">新品</span>
              <span v-if="product.isHot" class="tag hot">热卖</span>
              <span v-if="product.discount" class="tag discount">{{ product.discount }}折</span>
            </div>
          </div>
          <div class="product-info">
            <h3 class="product-name">{{ product.name }}</h3>
            <p class="product-desc">{{ product.description }}</p>
            <div class="product-price">
              <span class="current-price">¥{{ product.price }}</span>
              <span v-if="product.originalPrice" class="original-price">
                ¥{{ product.originalPrice }}
              </span>
            </div>
            <div class="product-actions">
              <el-button type="primary" size="small" @click.stop="addToCart(product)">
                <el-icon><ShoppingCart /></el-icon>
                加入购物车
              </el-button>
              <el-button size="small" @click.stop="$router.push(`/products/${product.id}`)">
                查看详情
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 促销活动 -->
    <div class="promotion-section">
      <h2 class="section-title">限时促销</h2>
      <div class="promotion-list">
        <div
          v-for="promotion in promotions"
          :key="promotion.id"
          class="promotion-card"
          :style="{ backgroundImage: `url(${promotion.background})` }"
        >
          <div class="promotion-content">
            <h3>{{ promotion.title }}</h3>
            <p>{{ promotion.description }}</p>
            <div class="countdown">
              <span>剩余时间：</span>
              <el-countdown
                :value="promotion.endTime"
                :format="'HH:mm:ss'"
                value-style="font-size: 20px; color: #f56c6c;"
              />
            </div>
            <el-button type="danger" size="large">
              立即抢购
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 品牌推荐 -->
    <div class="brand-section">
      <h2 class="section-title">品牌精选</h2>
      <div class="brand-list">
        <div
          v-for="brand in brands"
          :key="brand.id"
          class="brand-item"
        >
          <img :src="brand.logo" :alt="brand.name" />
          <span>{{ brand.name }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, markRaw } from 'vue'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  ShoppingCart,
  House,
  Lollipop,
  IceDrink,
  CoffeeCup,
  IceCream,
  Dessert,
  Food,
  GobletSquareFull
} from '@element-plus/icons-vue'
import { useProductStore } from '@/store/product'
import { useCartStore } from '@/store/cart'

// Store
const productStore = useProductStore()
const cartStore = useCartStore()

// 轮播图数据
const banners = ref([
  {
    id: 1,
    image: 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%221200%22%20height%3D%22400%22%3E%3Crect%20width%3D%221200%22%20height%3D%22400%22%20fill%3D%22%23409EFF%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2260%22%20fill%3D%22white%22%20text-anchor%3D%22middle%22%20dominant-baseline%3D%22central%22%3EBanner1%3C%2Ftext%3E%3C%2Fsvg%3E',
    title: '春季大促销',
    description: '全场商品5折起，限时抢购'
  },
  {
    id: 2,
    image: 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%221200%22%20height%3D%22400%22%3E%3Crect%20width%3D%221200%22%20height%3D%22400%22%20fill%3D%22%2367C23A%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2260%22%20fill%3D%22white%22%20text-anchor%3D%22middle%22%20dominant-baseline%3D%22central%22%3EBanner2%3C%2Ftext%3E%3C%2Fsvg%3E',
    title: '新品上市',
    description: '最新科技产品，抢先体验'
  },
  {
    id: 3,
    image: 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%221200%22%20height%3D%22400%22%3E%3Crect%20width%3D%221200%22%20height%3D%22400%22%20fill%3D%22%23E6A23C%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2260%22%20fill%3D%22white%22%20text-anchor%3D%22middle%22%20dominant-baseline%3D%22central%22%3EBanner3%3C%2Ftext%3E%3C%2Fsvg%3E',
    title: '会员专享',
    description: '会员尊享特价，更多优惠'
  }
])

// 分类数据 - 使用markRaw标记组件避免响应式警告
const categories = ref([
  { id: 1, name: '家用电器', icon: markRaw(House) },
  { id: 2, name: '数码产品', icon: markRaw(Lollipop) },
  { id: 3, name: '食品饮料', icon: markRaw(IceDrink) },
  { id: 4, name: '美妆个护', icon: markRaw(CoffeeCup) },
  { id: 5, name: '服装服饰', icon: markRaw(IceCream) },
  { id: 6, name: '家居生活', icon: markRaw(Dessert) },
  { id: 7, name: '运动户外', icon: markRaw(Food) },
  { id: 8, name: '图书音像', icon: markRaw(GobletSquareFull) }
])

// 推荐商品
const featuredProducts = ref([
  {
    id: 1,
    name: '智能手表',
    description: '多功能运动健康监测',
    image: 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22300%22%20height%3D%22300%22%3E%3Crect%20width%3D%22300%22%20height%3D%22300%22%20fill%3D%22%23409EFF%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2230%22%20fill%3D%22white%22%20text-anchor%3D%22middle%22%20dominant-baseline%3D%22central%22%3EProduct1%3C%2Ftext%3E%3C%2Fsvg%3E',
    price: 1299,
    originalPrice: 1599,
    isNew: true,
    isHot: true,
    discount: 8
  },
  {
    id: 2,
    name: '无线耳机',
    description: '主动降噪蓝牙耳机',
    image: 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22300%22%20height%3D%22300%22%3E%3Crect%20width%3D%22300%22%20height%3D%22300%22%20fill%3D%22%2367C23A%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2230%22%20fill%3D%22white%22%20text-anchor%3D%22middle%22%20dominant-baseline%3D%22central%22%3EProduct2%3C%2Ftext%3E%3C%2Fsvg%3E',
    price: 699,
    originalPrice: 899,
    isHot: true,
    discount: 7.8
  },
  {
    id: 3,
    name: '笔记本电脑',
    description: '高性能轻薄本',
    image: 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22300%22%20height%3D%22300%22%3E%3Crect%20width%3D%22300%22% height%3D%22300%22%20fill%3D%22%23E6A23C%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2230%22%20fill%3D%22white%22%20text-anchor%3D%22middle%22%20dominant-baseline%3D%22central%22%3EProduct3%3C%2Ftext%3E%3C%2Fsvg%3E',
    price: 5999,
    originalPrice: 6999,
    isNew: true,
    discount: 8.5
  },
  {
    id: 4,
    name: '智能手机',
    description: '旗舰级摄影手机',
    image: 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22300%22%20height%3D%22300%22%3E%3Crect%20width%3D%22300%22%20height%3D%22300%22%20fill%3D%22%23F56C6C%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2230%22%20fill%3D%22white%22%20text-anchor%3D%22middle%22%20dominant-baseline%3D%22central%22%3EProduct4%3C%2Ftext%3E%3C%2Fsvg%3E',
    price: 3999,
    originalPrice: 4999,
    isHot: true,
    discount: 8
  }
])

// 促销活动
const promotions = ref([
  {
    id: 1,
    title: '双十一预售',
    description: '预付定金立减100元',
    background: 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22600%22%20height%3D%22300%22%3E%3Crect%20width%3D%22600%22%20height%3D%22300%22%20fill%3D%22%23FF6B6B%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2240%22%20fill%3D%22white%22%20text-anchor%3D%22middle%22%20dominant-baseline%3D%22central%22%3EPromotion1%3C%2Ftext%3E%3C%2Fsvg%3E',
    endTime: Date.now() + 1000 * 60 * 60 * 24 * 3 // 3天后
  },
  {
    id: 2,
    title: '周年庆典',
    description: '全场满300减50',
    background: 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22600%22%20height%3D%22300%22%3E%3Crect%20width%3D%22600%22%20height%3D%22300%22%20fill%3D%22%234ECDC4%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2240%22%20fill%3D%22white%22% text-anchor%3D%22middle%22%20dominant-baseline%3D%22central%22%3EPromotion2%3C%2Ftext%3E%3C%2Fsvg%3E',
    endTime: Date.now() + 1000 * 60 * 60 * 24 * 5 // 5天后
  }
])

// 品牌数据
const brands = ref([
  { id: 1, name: '华为', logo: 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22100%22%20height%3D%2250%22%3E%3Crect%20width%3D%22100%22%20height%3D%2250%22%20fill%3D%22%23409EFF%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2212%22%20fill%3D%22white%22%20text-anchor%3D%22middle%22%20dominant-baseline%3D%22central%22%3EHuawei%3C%2Ftext%3E%3C%2Fsvg%3E' },
  { id: 2, name: '小米', logo: 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22100%22%20height%3D%2250%22%3E%3Crect%20width%3D%22100%22%20height%3D%2250%22%20fill%3D%22%2367C23A%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2212%22%20fill%3D%22white%22%20text-anchor%3D%22middle%22%20dominant-baseline%3D%22central%22%3EXiaomi%3C%2Ftext%3E%3C%2Fsvg%3E' },
  { id: 3, name: '苹果', logo: 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22100%22%20height%3D%2250%22%3E%3Crect%20width%3D%22100%22%20height%3D%2250%22%20fill%3D%22%23909399%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2212%22%20fill%3D%22white%22%20text-anchor%3D%22middle%22%20dominant-baseline%3D%22central%22%3EApple%3C%2Ftext%3E%3C%2Fsvg%3E' },
  { id: 4, name: '三星', logo: 'data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22100%22%20height%3D%2250%22%3E%3Crect%20width%3D%22100%22%20height%3D%2250%22%20fill%3D%22%23E6A23C%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20font-family%3D%22Arial%22%20font-size%3D%2212%22%20fill%3D%22white%22%20text-anchor%3D%22middle%22%20dominant-baseline%3D%22central%22%3ESamsung%3C%2Ftext%3E%3C%2Fsvg%3E' },
  { id: 5, name: '索尼', logo: 'https://via.placeholder.com/100x50/F56C6C/fff?text=Sony' }
])

// 方法
const addToCart = async (product) => {
  try {
    await cartStore.addToCart(product, 1)
  } catch (error) {
    console.error('添加到购物车失败:', error)
  }
}

onMounted(async () => {
  // 加载推荐商品
  await productStore.loadFeaturedProducts()
  // 如果store中有推荐商品数据，使用它
  if (productStore.featuredProducts.length > 0) {
    featuredProducts.value = productStore.featuredProducts
  }
})
</script>

<style scoped>
.home-page {
  padding-bottom: var(--space-xl);
}

/* 轮播图 */
.banner-section {
  margin-bottom: var(--space-xl);
}

.banner-item {
  height: 100%;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

.banner-content {
  text-align: center;
  color: white;
  background-color: rgba(0, 0, 0, 0.5);
  padding: var(--space-xl);
  border-radius: var(--border-radius);
  max-width: 600px;
}

.banner-content h2 {
  font-size: var(--font-size-xl);
  margin-bottom: var(--space-md);
  color: white;
}

.banner-content p {
  font-size: var(--font-size-md);
  margin-bottom: var(--space-lg);
  opacity: 0.9;
}

/* 分类导航 */
.category-section {
  margin-bottom: var(--space-xl);
}

.category-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: var(--space-lg);
  margin-top: var(--space-lg);
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-md);
  border-radius: var(--border-radius);
  background-color: var(--card-bg-color);
  border: 1px solid var(--border-color-light);
  cursor: pointer;
  transition: var(--transition-all);
}

.category-item:hover {
  transform: translateY(-4px);
  box-shadow: var(--box-shadow);
  border-color: var(--primary-color);
}

.category-icon {
  margin-bottom: var(--space-sm);
  color: var(--primary-color);
}

.category-name {
  font-size: var(--font-size-sm);
  color: var(--text-regular);
}

/* 推荐商品 */
.featured-section {
  margin-bottom: var(--space-xl);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-lg);
}

.section-title {
  font-size: var(--font-size-lg);
  color: var(--text-primary);
  margin: 0;
}

.view-all {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--primary-color);
  text-decoration: none;
  font-size: var(--font-size-sm);
}

.view-all:hover {
  text-decoration: underline;
}

.product-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--space-lg);
}

.product-card {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  overflow: hidden;
  cursor: pointer;
  transition: var(--transition-all);
}

.product-card:hover {
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

.product-price {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
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

.product-actions {
  display: flex;
  gap: var(--space-sm);
}

.product-actions .el-button {
  flex: 1;
}

/* 促销活动 */
.promotion-section {
  margin-bottom: var(--space-xl);
}

.promotion-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: var(--space-lg);
  margin-top: var(--space-lg);
}

.promotion-card {
  height: 300px;
  border-radius: var(--border-radius);
  background-size: cover;
  background-position: center;
  position: relative;
  overflow: hidden;
}

.promotion-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
}

.promotion-content {
  position: relative;
  z-index: 1;
  color: white;
  padding: var(--space-xl);
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.promotion-content h3 {
  font-size: var(--font-size-xl);
  margin-bottom: var(--space-md);
  color: white;
}

.promotion-content p {
  font-size: var(--font-size-md);
  margin-bottom: var(--space-lg);
  opacity: 0.9;
}

.countdown {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-lg);
  font-size: var(--font-size-md);
}

/* 品牌推荐 */
.brand-list {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: var(--space-xl);
  margin-top: var(--space-lg);
}

.brand-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
}

.brand-item img {
  width: 100px;
  height: 50px;
  object-fit: contain;
  filter: grayscale(100%);
  transition: var(--transition-all);
}

.brand-item:hover img {
  filter: grayscale(0%);
}

.brand-item span {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .promotion-list {
    grid-template-columns: 1fr;
  }

  .product-list {
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  }

  .category-list {
    grid-template-columns: repeat(4, 1fr);
  }

  .brand-list {
    gap: var(--space-lg);
  }

  .brand-item img {
    width: 80px;
    height: 40px;
  }
}
</style>