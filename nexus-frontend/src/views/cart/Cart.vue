<template>
  <div class="cart-page">
    <!-- 面包屑导航 -->
    <div class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>购物车</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="cart-content">
      <!-- 购物车为空 -->
      <div v-if="cartItems.length === 0" class="empty-cart">
        <el-empty description="购物车是空的">
          <template #image>
            <el-icon :size="100"><ShoppingCart /></el-icon>
          </template>
          <el-button type="primary" @click="$router.push('/products')">
            去逛逛
          </el-button>
        </el-empty>
      </div>

      <!-- 购物车有商品 -->
      <div v-else class="cart-with-items">
        <div class="cart-main">
          <!-- 购物车头部 -->
          <div class="cart-header">
            <div class="header-left">
              <el-checkbox
                v-model="selectAll"
                :indeterminate="isIndeterminate"
                @change="handleSelectAllChange"
              >
                全选
              </el-checkbox>
            </div>
            <div class="header-right">
              <el-button type="text" @click="handleClearCart">
                清空购物车
              </el-button>
            </div>
          </div>

          <!-- 购物车商品列表 -->
          <div class="cart-items">
            <div
              v-for="item in cartItems"
              :key="item.id"
              class="cart-item"
              :class="{ disabled: !item.available }"
            >
              <div class="item-select">
                <el-checkbox
                  :model-value="item.selected"
                  :disabled="!item.available"
                  @change="(value) => {
                    item.selected = value
                    handleItemSelectChange(item)
                  }"
                />
              </div>
              <div class="item-info">
                <div class="item-image" @click="$router.push(`/products/${item.productId}`)">
                  <img :src="item.image" :alt="item.name" />
                  <div v-if="!item.available" class="out-of-stock">已失效</div>
                </div>
                <div class="item-details">
                  <h3 class="item-name" @click="$router.push(`/products/${item.productId}`)">
                    {{ item.name }}
                  </h3>
                  <div class="item-spec">
                    <span v-for="spec in item.specs" :key="spec.name" class="spec-item">
                      {{ spec.name }}: {{ spec.value }}
                    </span>
                  </div>
                  <div class="item-promotion" v-if="item.promotions.length > 0">
                    <el-tag
                      v-for="promotion in item.promotions"
                      :key="promotion.id"
                      :type="promotion.type"
                      size="small"
                    >
                      {{ promotion.text }}
                    </el-tag>
                  </div>
                </div>
              </div>
              <div class="item-price">
                <div class="price-info">
                  <div class="current-price">¥{{ item.price }}</div>
                  <div v-if="item.originalPrice" class="original-price">
                    ¥{{ item.originalPrice }}
                  </div>
                </div>
              </div>
              <div class="item-quantity">
                <el-input-number
                  v-model="item.quantity"
                  :min="1"
                  :max="item.stock"
                  :disabled="!item.available"
                  controls-position="right"
                  size="small"
                  @change="handleQuantityChange(item)"
                />
                <div v-if="item.available" class="stock-info">
                  库存: {{ item.stock }}
                </div>
              </div>
              <div class="item-subtotal">
                <div class="subtotal">¥{{ item.subtotal }}</div>
                <div v-if="item.saveAmount > 0" class="save-amount">
                  节省: ¥{{ item.saveAmount }}
                </div>
              </div>
              <div class="item-actions">
                <el-button
                  type="text"
                  :icon="Delete"
                  @click="handleRemoveItem(item.id)"
                >
                  删除
                </el-button>
                <el-button
                  type="text"
                  :icon="Star"
                  @click="handleMoveToWishlist(item)"
                >
                  移到收藏
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 购物车侧边栏 -->
        <div class="cart-sidebar">
          <div class="sidebar-card">
            <h3 class="sidebar-title">订单摘要</h3>

            <!-- 商品合计 -->
            <div class="summary-item">
              <span class="label">商品合计</span>
              <span class="value">¥{{ summary.totalAmount }}</span>
            </div>

            <!-- 优惠折扣 -->
            <div class="summary-item discount">
              <span class="label">优惠折扣</span>
              <span class="value">-¥{{ summary.discountAmount }}</span>
            </div>

            <!-- 运费 -->
            <div class="summary-item">
              <span class="label">运费</span>
              <span class="value">
                <span v-if="summary.shippingFee === 0">免运费</span>
                <span v-else>¥{{ summary.shippingFee }}</span>
              </span>
            </div>

            <!-- 促销活动 -->
            <div class="promotion-section">
              <div class="promotion-header">
                <span class="label">促销活动</span>
                <el-button type="text" @click="showPromotionDialog = true">
                  选择优惠券
                </el-button>
              </div>
              <div class="promotion-list">
                <div v-for="promotion in appliedPromotions" :key="promotion.id" class="promotion-item">
                  <span class="promotion-text">{{ promotion.text }}</span>
                  <span class="promotion-value">-¥{{ promotion.discount }}</span>
                  <el-icon @click="removePromotion(promotion.id)"><Close /></el-icon>
                </div>
              </div>
            </div>

            <!-- 合计金额 -->
            <div class="summary-total">
              <span class="label">实付金额</span>
              <span class="value total-amount">¥{{ summary.finalAmount }}</span>
            </div>

            <!-- 结算按钮 -->
            <div class="checkout-actions">
              <el-button
                type="primary"
                size="large"
                :disabled="selectedCount === 0"
                @click="handleCheckout"
              >
                去结算 ({{ selectedCount }}件)
              </el-button>
            </div>

            <!-- 购物提示 -->
            <div class="shopping-tips">
              <div class="tip-item">
                <el-icon><InfoFilled /></el-icon>
                <span>支持7天无理由退货</span>
              </div>
              <div class="tip-item">
                <el-icon><InfoFilled /></el-icon>
                <span>全场满¥99包邮</span>
              </div>
              <div class="tip-item">
                <el-icon><InfoFilled /></el-icon>
                <span>商品价格和库存可能变动，请及时结算</span>
              </div>
            </div>
          </div>

          <!-- 推荐商品 -->
          <div class="recommended-products">
            <h3 class="sidebar-title">猜你喜欢</h3>
            <div class="recommended-list">
              <div
                v-for="product in recommendedProducts"
                :key="product.id"
                class="recommended-item"
                @click="$router.push(`/products/${product.id}`)"
              >
                <img :src="product.image" :alt="product.name" />
                <div class="recommended-info">
                  <h4>{{ product.name }}</h4>
                  <div class="price">¥{{ product.price }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 优惠券对话框 -->
    <el-dialog
      v-model="showPromotionDialog"
      title="选择优惠券"
      width="500px"
    >
      <div class="coupon-dialog">
        <div class="coupon-list">
          <div
            v-for="coupon in availableCoupons"
            :key="coupon.id"
            class="coupon-item"
            :class="{ active: selectedCouponId === coupon.id }"
            @click="selectCoupon(coupon)"
          >
            <div class="coupon-info">
              <div class="coupon-amount">
                <span class="currency">¥</span>
                <span class="amount">{{ coupon.amount }}</span>
              </div>
              <div class="coupon-details">
                <div class="coupon-name">{{ coupon.name }}</div>
                <div class="coupon-condition">满{{ coupon.minAmount }}元可用</div>
                <div class="coupon-expiry">有效期至 {{ coupon.expiryDate }}</div>
              </div>
            </div>
            <div class="coupon-action">
              <el-button
                v-if="selectedCouponId === coupon.id"
                type="primary"
                size="small"
                @click.stop="applyCoupon(coupon)"
              >
                使用
              </el-button>
              <el-button
                v-else
                type="text"
                size="small"
                @click.stop="selectCoupon(coupon)"
              >
                选择
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showPromotionDialog = false">取消</el-button>
          <el-button type="primary" @click="showPromotionDialog = false">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ShoppingCart,
  Delete,
  Star,
  Close,
  InfoFilled
} from '@element-plus/icons-vue'

// 路由
const router = useRouter()

// Pinia store
import { useCartStore } from '@/store/cart'
import { useFavoriteStore } from '@/store/favorite'
import { useUserStore } from '@/store/user'
const cartStore = useCartStore()
const favoriteStore = useFavoriteStore()
const userStore = useUserStore()

// 状态
const cartItems = computed(() => cartStore.items)

const appliedPromotions = ref([
  { id: 1, text: '店铺优惠券', discount: 50 },
  { id: 2, text: '平台满减', discount: 30 }
])

const availableCoupons = ref([
  {
    id: 1,
    name: '新人专享券',
    amount: 100,
    minAmount: 1000,
    expiryDate: '2026-12-31'
  },
  {
    id: 2,
    name: '店铺通用券',
    amount: 50,
    minAmount: 500,
    expiryDate: '2026-12-31'
  },
  {
    id: 3,
    name: '节日大促券',
    amount: 200,
    minAmount: 2000,
    expiryDate: '2026-12-31'
  }
])

const selectedCouponId = ref(null)
const showPromotionDialog = ref(false)

// 计算属性
const selectAll = computed({
  get: () => cartStore.isAllSelected,
  set: (value) => cartStore.selectAll(value)
})

const isIndeterminate = computed(() => cartStore.isIndeterminate)
const selectedItems = computed(() => cartStore.selectedItems)
const selectedCount = computed(() => cartStore.selectedCount)

const summary = computed(() => {
  const totalAmount = cartStore.totalAmount
  const discountAmount = cartStore.discountAmount

  // 促销优惠
  const promotionDiscount = appliedPromotions.value.reduce((total, promo) => {
    return total + promo.discount
  }, 0)

  // 运费 (满99包邮)
  const shippingFee = totalAmount >= 99 ? 0 : 10

  // 最终金额
  const finalAmount = totalAmount + shippingFee - promotionDiscount

  return {
    totalAmount,
    discountAmount,
    shippingFee,
    promotionDiscount,
    finalAmount
  }
})

// 方法
const handleSelectAllChange = (value) => {
  cartStore.selectAll(value)
}

const handleItemSelectChange = async (item) => {
  await cartStore.updateSelection(item.id, item.selected)
}

const handleQuantityChange = async (item) => {
  await cartStore.updateQuantity(item.id, item.quantity)
}

const handleRemoveItem = async (itemId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await cartStore.removeItem(itemId)
  } catch {
    // 用户取消
  }
}

const handleMoveToWishlist = async (item) => {
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    await ElMessageBox.confirm('确定要移到收藏夹吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })

    // 调用API移到收藏夹
    await favoriteStore.moveFromCart(item.productId, item.id)
    // 移动成功后，favoriteStore会更新，cartStore需要重新加载
    await cartStore.loadCart()
  } catch {
    // 用户取消
  }
}

const handleClearCart = async () => {
  try {
    await ElMessageBox.confirm('确定要清空购物车吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await cartStore.clearCart()
  } catch {
    // 用户取消
  }
}

const handleCheckout = () => {
  if (selectedCount.value === 0) {
    ElMessage.warning('请选择要结算的商品')
    return
  }

  // 跳转到结算页面
  router.push('/checkout')
}

const selectCoupon = (coupon) => {
  selectedCouponId.value = coupon.id
}

const applyCoupon = (coupon) => {
  if (summary.value.totalAmount < coupon.minAmount) {
    ElMessage.warning(`该优惠券需满${coupon.minAmount}元才能使用`)
    return
  }

  // 添加优惠券
  appliedPromotions.value.push({
    id: coupon.id,
    text: coupon.name,
    discount: coupon.amount
  })

  selectedCouponId.value = null
  ElMessage.success('优惠券已应用')
}

const removePromotion = (promotionId) => {
  appliedPromotions.value = appliedPromotions.value.filter(
    promo => promo.id !== promotionId
  )
  ElMessage.success('已移除优惠')
}

// 推荐商品
const recommendedProducts = ref([
  {
    id: 4,
    name: '运动手环',
    image: '/images/product4.jpg',
    price: 299
  },
  {
    id: 5,
    name: '蓝牙音箱',
    image: '/images/product5.jpg',
    price: 499
  },
  {
    id: 6,
    name: '智能摄像头',
    image: '/images/product6.jpg',
    price: 399
  }
])

onMounted(() => {
  cartStore.loadCart()
})

// 监听购物车商品选择状态变化
watch(cartItems, (newItems) => {
  newItems.forEach(item => {
    // 这里不需要单独处理，因为item.selected的变化会直接反映在store的items中
    // store的getter会基于items重新计算
  })
}, { deep: true })
</script>

<style scoped>
.cart-page {
  padding-bottom: var(--space-xl);
}

.breadcrumb {
  margin-bottom: var(--space-lg);
}

/* 空购物车 */
.empty-cart {
  text-align: center;
  padding: var(--space-xl);
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
}

/* 购物车有商品 */
.cart-with-items {
  display: flex;
  gap: var(--space-lg);
}

.cart-main {
  flex: 1;
}

.cart-sidebar {
  width: 350px;
  flex-shrink: 0;
}

/* 购物车头部 */
.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md);
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius) var(--border-radius) 0 0;
  border: 1px solid var(--border-color-light);
  border-bottom: none;
}

/* 购物车商品列表 */
.cart-items {
  background-color: var(--card-bg-color);
  border-radius: 0 0 var(--border-radius) var(--border-radius);
  border: 1px solid var(--border-color-light);
}

.cart-item {
  display: flex;
  align-items: center;
  padding: var(--space-lg);
  border-bottom: 1px solid var(--border-color-light);
  transition: var(--transition-all);
}

.cart-item:last-child {
  border-bottom: none;
}

.cart-item:hover {
  background-color: var(--bg-color);
}

.cart-item.disabled {
  opacity: 0.6;
  background-color: var(--bg-color);
}

.item-select {
  width: 50px;
  text-align: center;
}

.item-info {
  flex: 1;
  display: flex;
  gap: var(--space-md);
}

.item-image {
  position: relative;
  width: 100px;
  height: 100px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.out-of-stock {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-sm);
  font-weight: bold;
}

.item-details {
  flex: 1;
}

.item-name {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
  cursor: pointer;
}

.item-name:hover {
  color: var(--primary-color);
}

.item-spec {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin-bottom: var(--space-xs);
}

.spec-item {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  padding: 2px 8px;
  background-color: var(--bg-color);
  border-radius: var(--border-radius);
}

.item-promotion {
  display: flex;
  gap: var(--space-xs);
  margin-top: var(--space-xs);
}

.item-price {
  width: 150px;
  text-align: right;
}

.price-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.current-price {
  font-size: var(--font-size-md);
  color: var(--danger-color);
  font-weight: bold;
  margin-bottom: var(--space-xs);
}

.original-price {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  text-decoration: line-through;
}

.item-quantity {
  width: 120px;
  text-align: center;
}

.stock-info {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-top: var(--space-xs);
}

.item-subtotal {
  width: 150px;
  text-align: right;
}

.subtotal {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  font-weight: bold;
  margin-bottom: var(--space-xs);
}

.save-amount {
  font-size: var(--font-size-sm);
  color: var(--success-color);
}

.item-actions {
  width: 120px;
  text-align: center;
}

.item-actions .el-button {
  display: block;
  margin-bottom: var(--space-xs);
}

/* 侧边栏 */
.sidebar-card {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  padding: var(--space-lg);
  margin-bottom: var(--space-lg);
}

.sidebar-title {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin-bottom: var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: 1px solid var(--border-color-light);
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-md);
}

.summary-item .label {
  color: var(--text-regular);
}

.summary-item .value {
  color: var(--text-primary);
  font-weight: 500;
}

.summary-item.discount .value {
  color: var(--success-color);
}

.promotion-section {
  margin: var(--space-lg) 0;
  padding: var(--space-md);
  background-color: var(--bg-color);
  border-radius: var(--border-radius);
}

.promotion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-sm);
}

.promotion-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.promotion-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--font-size-sm);
}

.promotion-text {
  color: var(--text-regular);
}

.promotion-value {
  color: var(--success-color);
  font-weight: 500;
}

.promotion-item .el-icon {
  color: var(--text-secondary);
  cursor: pointer;
}

.promotion-item .el-icon:hover {
  color: var(--danger-color);
}

.summary-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: var(--space-lg) 0;
  padding-top: var(--space-md);
  border-top: 2px solid var(--border-color-light);
}

.total-amount {
  font-size: 24px;
  color: var(--danger-color);
  font-weight: bold;
}

.checkout-actions {
  margin: var(--space-lg) 0;
}

.checkout-actions .el-button {
  width: 100%;
}

.shopping-tips {
  margin-top: var(--space-lg);
  padding-top: var(--space-md);
  border-top: 1px solid var(--border-color-light);
}

.tip-item {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-xs);
}

.tip-item .el-icon {
  color: var(--info-color);
}

/* 推荐商品 */
.recommended-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.recommended-item {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-sm);
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: var(--transition-all);
}

.recommended-item:hover {
  background-color: var(--bg-color);
}

.recommended-item img {
  width: 60px;
  height: 60px;
  border-radius: var(--border-radius);
  object-fit: cover;
  flex-shrink: 0;
}

.recommended-info {
  flex: 1;
}

.recommended-info h4 {
  font-size: var(--font-size-sm);
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommended-info .price {
  color: var(--danger-color);
  font-weight: bold;
  font-size: var(--font-size-sm);
}

/* 优惠券对话框 */
.coupon-dialog {
  max-height: 400px;
  overflow-y: auto;
}

.coupon-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.coupon-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md);
  border: 2px solid var(--border-color);
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: var(--transition-all);
}

.coupon-item.active {
  border-color: var(--primary-color);
  background-color: rgba(64, 158, 255, 0.05);
}

.coupon-item:hover {
  border-color: var(--primary-color);
}

.coupon-info {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.coupon-amount {
  display: flex;
  align-items: baseline;
}

.coupon-amount .currency {
  font-size: var(--font-size-sm);
  color: var(--danger-color);
  font-weight: bold;
}

.coupon-amount .amount {
  font-size: 24px;
  color: var(--danger-color);
  font-weight: bold;
}

.coupon-details {
  display: flex;
  flex-direction: column;
}

.coupon-name {
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
}

.coupon-condition {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-xs);
}

.coupon-expiry {
  font-size: var(--font-size-xs);
  color: var(--text-placeholder);
}

/* 响应式设计 */
@media (max-width: 992px) {
  .cart-with-items {
    flex-direction: column;
  }

  .cart-sidebar {
    width: 100%;
  }

  .cart-item {
    flex-wrap: wrap;
    gap: var(--space-md);
  }

  .item-info {
    order: 1;
    width: 100%;
  }

  .item-price {
    order: 2;
    width: auto;
    text-align: left;
  }

  .item-quantity {
    order: 3;
    width: auto;
  }

  .item-subtotal {
    order: 4;
    width: auto;
    text-align: right;
    margin-left: auto;
  }

  .item-actions {
    order: 5;
    width: 100%;
    text-align: left;
    display: flex;
    gap: var(--space-md);
  }
}

@media (max-width: 768px) {
  .cart-item {
    padding: var(--space-md);
  }

  .item-image {
    width: 80px;
    height: 80px;
  }

  .item-price,
  .item-quantity,
  .item-subtotal {
    width: 100%;
    text-align: left;
    margin-bottom: var(--space-sm);
  }
}
</style>