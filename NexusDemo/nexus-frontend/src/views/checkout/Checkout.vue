<template>
  <div class="checkout-page">
    <!-- 面包屑导航 -->
    <div class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/cart' }">购物车</el-breadcrumb-item>
        <el-breadcrumb-item>结算</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">结算</h1>
      <div class="page-subtitle">请确认订单信息并完成支付</div>
    </div>

    <!-- 结算内容 -->
    <div class="checkout-content">
      <!-- 左侧表单 -->
      <div class="checkout-form">
        <!-- 收货地址 -->
        <div class="section">
          <div class="section-header">
            <h2 class="section-title">
              <el-icon><Location /></el-icon>
              收货地址
            </h2>
            <el-button type="text" @click="handleManageAddress">
              管理地址
            </el-button>
          </div>
          <div class="section-body">
            <div v-if="selectedAddress" class="address-card selected">
              <div class="address-info">
                <div class="receiver">
                  <span class="name">{{ selectedAddress.receiverName }}</span>
                  <span class="phone">{{ selectedAddress.receiverPhone }}</span>
                  <el-tag v-if="selectedAddress.isDefault" size="small">默认</el-tag>
                </div>
                <div class="address">
                  {{ selectedAddress.province }}
                  {{ selectedAddress.city }}
                  {{ selectedAddress.district }}
                  {{ selectedAddress.detailAddress }}
                </div>
              </div>
              <div class="address-actions">
                <el-button type="text" @click="handleChangeAddress">
                  修改
                </el-button>
              </div>
            </div>

            <!-- 地址列表 -->
            <div v-if="showAddressList" class="address-list">
              <div
                v-for="address in addresses"
                :key="address.id"
                class="address-card"
                :class="{ selected: address.id === selectedAddressId }"
                @click="selectAddress(address)"
              >
                <div class="address-info">
                  <div class="receiver">
                    <span class="name">{{ address.receiverName }}</span>
                    <span class="phone">{{ address.receiverPhone }}</span>
                    <el-tag v-if="address.isDefault" size="small">默认</el-tag>
                  </div>
                  <div class="address">
                    {{ address.province }}
                    {{ address.city }}
                    {{ address.district }}
                    {{ address.detailAddress }}
                  </div>
                </div>
                <div class="address-actions">
                  <el-button type="text" @click.stop="handleEditAddress(address)">
                    编辑
                  </el-button>
                </div>
              </div>

              <div class="add-address" @click="handleAddAddress">
                <el-icon><Plus /></el-icon>
                <span>添加新地址</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 配送方式 -->
        <div class="section">
          <div class="section-header">
            <h2 class="section-title">
              <el-icon><Truck /></el-icon>
              配送方式
            </h2>
          </div>
          <div class="section-body">
            <el-radio-group v-model="selectedShippingMethod">
              <el-radio
                v-for="method in shippingMethods"
                :key="method.value"
                :value="method.value"
                border
              >
                <div class="shipping-method">
                  <div class="method-name">{{ method.label }}</div>
                  <div class="method-desc">{{ method.desc }}</div>
                  <div class="method-fee">
                    <span v-if="method.fee === 0">免费</span>
                    <span v-else>¥{{ method.fee }}</span>
                  </div>
                </div>
              </el-radio>
            </el-radio-group>
          </div>
        </div>

        <!-- 支付方式 -->
        <div class="section">
          <div class="section-header">
            <h2 class="section-title">
              <el-icon><CreditCard /></el-icon>
              支付方式
            </h2>
          </div>
          <div class="section-body">
            <el-radio-group v-model="selectedPaymentMethod">
              <el-radio
                v-for="method in paymentMethods"
                :key="method.value"
                :value="method.value"
                border
              >
                <div class="payment-method">
                  <el-icon class="method-icon">
                    <component :is="method.icon" />
                  </el-icon>
                  <div class="method-info">
                    <div class="method-name">{{ method.label }}</div>
                    <div class="method-desc">{{ method.desc }}</div>
                  </div>
                </div>
              </el-radio>
            </el-radio-group>
          </div>
        </div>

        <!-- 商品清单 -->
        <div class="section">
          <div class="section-header">
            <h2 class="section-title">
              <el-icon><ShoppingCart /></el-icon>
              商品清单
            </h2>
            <el-button type="text" @click="$router.push('/cart')">
              返回修改
            </el-button>
          </div>
          <div class="section-body">
            <div class="product-list">
              <div
                v-for="item in selectedItems"
                :key="item.id"
                class="product-item"
              >
                <div class="product-image">
                  <img :src="item.image" :alt="item.name" />
                </div>
                <div class="product-info">
                  <h3 class="product-name">{{ item.name }}</h3>
                  <div class="product-specs">
                    <span
                      v-for="spec in item.specs"
                      :key="spec.name"
                      class="spec-item"
                    >
                      {{ spec.name }}: {{ spec.value }}
                    </span>
                  </div>
                </div>
                <div class="product-quantity">×{{ item.quantity }}</div>
                <div class="product-price">¥{{ item.price }}</div>
                <div class="product-subtotal">¥{{ item.price * item.quantity }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧摘要 -->
      <div class="checkout-summary">
        <div class="summary-card">
          <h3 class="summary-title">订单摘要</h3>

          <div class="summary-list">
            <div class="summary-item">
              <span class="label">商品件数</span>
              <span class="value">{{ selectedCount }}件</span>
            </div>
            <div class="summary-item">
              <span class="label">商品总价</span>
              <span class="value">¥{{ cartStore.totalAmount }}</span>
            </div>
            <div class="summary-item">
              <span class="label">商品优惠</span>
              <span class="value discount">-¥{{ cartStore.discountAmount }}</span>
            </div>
            <div class="summary-item">
              <span class="label">运费</span>
              <span class="value">
                <span v-if="shippingFee === 0">免费</span>
                <span v-else>¥{{ shippingFee }}</span>
              </span>
            </div>

            <!-- 优惠券 -->
            <div class="coupon-section">
              <div class="coupon-header" @click="showCouponDialog = true">
                <span class="label">优惠券</span>
                <div class="coupon-action">
                  <span v-if="selectedCoupon" class="coupon-selected">
                    {{ selectedCoupon.name }}
                  </span>
                  <span v-else class="coupon-placeholder">选择优惠券</span>
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
              <div v-if="selectedCoupon" class="coupon-info">
                <span class="coupon-text">{{ selectedCoupon.name }}</span>
                <span class="coupon-amount">-¥{{ selectedCoupon.amount }}</span>
                <el-icon @click="removeCoupon"><Close /></el-icon>
              </div>
            </div>

            <div class="summary-divider"></div>

            <div class="summary-total">
              <span class="label">应付总额</span>
              <span class="value total-amount">¥{{ finalAmount }}</span>
            </div>
          </div>

          <!-- 结算按钮 -->
          <div class="checkout-actions">
            <el-button
              type="primary"
              size="large"
              :loading="isSubmitting"
              :disabled="!canCheckout"
              @click="handleSubmitOrder"
            >
              提交订单
            </el-button>
          </div>

          <!-- 购物提示 -->
          <div class="shopping-tips">
            <div class="tip-item">
              <el-icon><InfoFilled /></el-icon>
              <span>提交订单后，请在24小时内完成支付</span>
            </div>
            <div class="tip-item">
              <el-icon><InfoFilled /></el-icon>
              <span>支持7天无理由退货</span>
            </div>
            <div class="tip-item">
              <el-icon><InfoFilled /></el-icon>
              <span>如有问题，请联系客服 400-123-4567</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 优惠券对话框 -->
    <el-dialog
      v-model="showCouponDialog"
      title="选择优惠券"
      width="500px"
    >
      <div class="coupon-dialog">
        <div class="coupon-list">
          <div
            v-for="coupon in availableCoupons"
            :key="coupon.id"
            class="coupon-item"
            :class="{ active: selectedCoupon?.id === coupon.id }"
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
                v-if="selectedCoupon?.id === coupon.id"
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
          <el-button @click="showCouponDialog = false">取消</el-button>
          <el-button type="primary" @click="showCouponDialog = false">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Location,
  Truck,
  CreditCard,
  ShoppingCart,
  Plus,
  ArrowRight,
  Close,
  InfoFilled
} from '@element-plus/icons-vue'
import { useCartStore } from '@/store/cart'
import { useOrderStore } from '@/store/order'
import { useUserStore } from '@/store/user'
import { useAddressStore } from '@/store/address'

// 路由
const router = useRouter()

// Store
const cartStore = useCartStore()
const orderStore = useOrderStore()
const userStore = useUserStore()
const addressStore = useAddressStore()

// 状态
const selectedAddressId = ref(null)
const showAddressList = ref(false)
const selectedShippingMethod = ref('standard')
const selectedPaymentMethod = ref('alipay')
const selectedCoupon = ref(null)
const showCouponDialog = ref(false)
const isSubmitting = ref(false)

// 地址数据 - 使用addressStore
const addresses = computed(() => addressStore.addresses)

// 配送方式
const shippingMethods = ref([
  { value: 'standard', label: '标准配送', desc: '3-5个工作日', fee: 0, minAmount: 99 },
  { value: 'express', label: '快递配送', desc: '1-2个工作日', fee: 10, minAmount: 0 },
  { value: 'overnight', label: '次日达', desc: '次日送达', fee: 20, minAmount: 0 }
])

// 支付方式
const paymentMethods = ref([
  { value: 'alipay', label: '支付宝', desc: '推荐支付宝用户使用', icon: 'Alipay' },
  { value: 'wechat', label: '微信支付', desc: '推荐微信用户使用', icon: 'Wechat' },
  { value: 'bank', label: '银行卡', desc: '支持储蓄卡/信用卡', icon: 'Bank' },
  { value: 'cash', label: '货到付款', desc: '收到商品后现金支付', icon: 'Money' }
])

// 优惠券数据
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

// 计算属性
const selectedAddress = computed(() => {
  if (selectedAddressId.value) {
    return addresses.value.find(addr => addr.id === selectedAddressId.value)
  }
  return addressStore.defaultAddress || addresses.value[0]
})

const selectedItems = computed(() => cartStore.selectedItems)
const selectedCount = computed(() => cartStore.selectedCount)

const shippingFee = computed(() => {
  const method = shippingMethods.value.find(m => m.value === selectedShippingMethod.value)
  if (!method) return 0

  // 如果满足免运费条件
  if (cartStore.totalAmount >= method.minAmount) {
    return 0
  }
  return method.fee
})

const couponDiscount = computed(() => {
  if (!selectedCoupon.value) return 0
  if (cartStore.totalAmount < selectedCoupon.value.minAmount) return 0
  return selectedCoupon.value.amount
})

const finalAmount = computed(() => {
  return cartStore.totalAmount + shippingFee.value - couponDiscount.value
})

const canCheckout = computed(() => {
  return selectedItems.value.length > 0 && selectedAddress.value
})

// 方法
const selectAddress = (address) => {
  selectedAddressId.value = address.id
  showAddressList.value = false
}

const handleManageAddress = () => {
  showAddressList.value = !showAddressList.value
}

const handleChangeAddress = () => {
  showAddressList.value = true
}

const handleAddAddress = () => {
  ElMessage.info('添加地址功能开发中')
}

const handleEditAddress = (address) => {
  ElMessage.info(`编辑地址: ${address.name}`)
}

const selectCoupon = (coupon) => {
  selectedCoupon.value = coupon
}

const applyCoupon = (coupon) => {
  if (cartStore.totalAmount < coupon.minAmount) {
    ElMessage.warning(`该优惠券需满${coupon.minAmount}元才能使用`)
    return
  }
  selectedCoupon.value = coupon
  showCouponDialog.value = false
  ElMessage.success('优惠券已应用')
}

const removeCoupon = () => {
  selectedCoupon.value = null
  ElMessage.info('已移除优惠券')
}

const handleSubmitOrder = async () => {
  try {
    isSubmitting.value = true

    // 验证
    if (selectedItems.value.length === 0) {
      ElMessage.warning('请选择要购买的商品')
      return
    }

    if (!selectedAddress.value) {
      ElMessage.warning('请选择收货地址')
      return
    }

    // 确认订单
    await ElMessageBox.confirm('确认提交订单吗？', '订单确认', {
      confirmButtonText: '确认提交',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 调试信息
    console.log('selectedAddress:', selectedAddress.value)
    console.log('selectedItems:', selectedItems.value)

    // 确保有选中的地址
    if (!selectedAddress.value) {
      ElMessage.warning('请选择收货地址')
      return
    }

    // 构建订单数据 - 适配后端 OrderDTO 格式
    const orderData = {
      // 后端需要：receiverName, receiverPhone, receiverAddress, cartItemIds
      receiverName: selectedAddress.value.receiverName,
      receiverPhone: selectedAddress.value.receiverPhone,
      receiverAddress: [
        selectedAddress.value.province,
        selectedAddress.value.city,
        selectedAddress.value.district,
        selectedAddress.value.detailAddress
      ].filter(Boolean).join(''),
      // 提取选中购物车项的ID
      cartItemIds: selectedItems.value.map(item => item.id)
    }

    console.log('orderData:', orderData)

    // 创建订单
    const order = await orderStore.createOrder(orderData)

    // 清除已结算的商品
    const itemsToRemove = [...selectedItems.value]
    for (const item of itemsToRemove) {
      await cartStore.removeItem(item.id)
    }

    // 跳转到订单详情页
    ElMessage.success('订单创建成功')
    router.push(`/orders/${order.id}`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交订单失败:', error)
      ElMessage.error('提交订单失败')
    }
  } finally {
    isSubmitting.value = false
  }
}

// 初始化
onMounted(async () => {
  // 检查用户是否登录
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  // 加载购物车数据
  try {
    await cartStore.loadCart()
  } catch (error) {
    console.error('加载购物车失败:', error)
  }

  // 加载地址数据
  try {
    await addressStore.loadAddresses()
    // 设置默认地址
    if (addressStore.defaultAddress) {
      selectedAddressId.value = addressStore.defaultAddress.id
    }
  } catch (error) {
    console.error('加载地址失败:', error)
  }

  console.log('购物车数据:', cartStore.items)
  console.log('选中商品:', selectedItems.value)

  // 检查购物车是否有商品
  if (cartStore.items.length === 0) {
    ElMessage.warning('购物车是空的，请先添加商品')
    router.push('/products')
    return
  }

  // 检查是否有选中的商品，如果没有则全选
  if (selectedItems.value.length === 0) {
    // 自动全选所有商品
    cartStore.selectAll(true)
  }

  // 设置默认地址
  const defaultAddress = addresses.value.find(addr => addr.isDefault)
  if (defaultAddress) {
    selectedAddressId.value = defaultAddress.id
  } else if (addresses.value.length > 0) {
    selectedAddressId.value = addresses.value[0].id
  }

  console.log('当前地址:', selectedAddress.value)
})
</script>

<style scoped>
.checkout-page {
  padding-bottom: var(--space-xl);
}

.page-header {
  margin-bottom: var(--space-lg);
}

.page-title {
  font-size: 24px;
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
}

.page-subtitle {
  font-size: var(--font-size-md);
  color: var(--text-secondary);
}

/* 结算内容 */
.checkout-content {
  display: flex;
  gap: var(--space-lg);
}

.checkout-form {
  flex: 1;
}

.checkout-summary {
  width: 350px;
  flex-shrink: 0;
}

/* 区块样式 */
.section {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  margin-bottom: var(--space-lg);
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md) var(--space-lg);
  background-color: var(--bg-color);
  border-bottom: 1px solid var(--border-color-light);
}

.section-title {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin: 0;
}

.section-body {
  padding: var(--space-lg);
}

/* 地址样式 */
.address-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: var(--space-md);
  border: 2px solid var(--border-color);
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: var(--transition-all);
}

.address-card.selected {
  border-color: var(--primary-color);
  background-color: rgba(64, 158, 255, 0.05);
}

.address-card:hover {
  border-color: var(--primary-color);
}

.address-info {
  flex: 1;
}

.receiver {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-xs);
}

.receiver .name {
  font-weight: 500;
  color: var(--text-primary);
}

.receiver .phone {
  color: var(--text-regular);
}

.address {
  color: var(--text-primary);
  line-height: 1.5;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
  margin-top: var(--space-md);
}

.add-address {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  padding: var(--space-lg);
  border: 2px dashed var(--border-color);
  border-radius: var(--border-radius);
  color: var(--text-secondary);
  cursor: pointer;
  transition: var(--transition-all);
}

.add-address:hover {
  border-color: var(--primary-color);
  color: var(--primary-color);
}

/* 配送方式 */
.shipping-method {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.method-name {
  font-weight: 500;
  color: var(--text-primary);
}

.method-desc {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.method-fee {
  font-weight: 500;
  color: var(--danger-color);
}

/* 支付方式 */
.payment-method {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.method-icon {
  font-size: 24px;
  color: var(--primary-color);
}

.method-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

/* 商品清单 */
.product-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.product-item {
  display: flex;
  align-items: center;
  padding: var(--space-md);
  border: 1px solid var(--border-color-light);
  border-radius: var(--border-radius);
}

.product-image {
  width: 60px;
  height: 60px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  overflow: hidden;
  margin-right: var(--space-md);
  flex-shrink: 0;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-info {
  flex: 1;
}

.product-name {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
}

.product-specs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
}

.spec-item {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  padding: 2px 8px;
  background-color: var(--bg-color);
  border-radius: var(--border-radius);
}

.product-quantity {
  width: 60px;
  text-align: center;
  color: var(--text-regular);
  margin: 0 var(--space-md);
}

.product-price {
  width: 80px;
  text-align: right;
  color: var(--danger-color);
  font-weight: bold;
}

.product-subtotal {
  width: 100px;
  text-align: right;
  font-weight: 500;
  color: var(--text-primary);
}

/* 订单摘要 */
.summary-card {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  padding: var(--space-lg);
  position: sticky;
  top: var(--space-lg);
}

.summary-title {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin-bottom: var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: 1px solid var(--border-color-light);
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.coupon-section {
  margin: var(--space-md) 0;
}

.coupon-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-sm);
  background-color: var(--bg-color);
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: var(--transition-all);
}

.coupon-header:hover {
  background-color: rgba(64, 158, 255, 0.1);
}

.coupon-action {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.coupon-selected {
  color: var(--primary-color);
  font-weight: 500;
}

.coupon-placeholder {
  color: var(--text-secondary);
}

.coupon-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-sm);
  background-color: rgba(103, 194, 58, 0.1);
  border-radius: var(--border-radius);
  margin-top: var(--space-xs);
}

.coupon-text {
  color: var(--success-color);
  font-weight: 500;
}

.coupon-amount {
  color: var(--success-color);
  font-weight: bold;
}

.coupon-info .el-icon {
  color: var(--text-secondary);
  cursor: pointer;
}

.coupon-info .el-icon:hover {
  color: var(--danger-color);
}

.summary-divider {
  height: 1px;
  background-color: var(--border-color-light);
  margin: var(--space-md) 0;
}

.summary-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-amount {
  font-size: 24px;
  color: var(--danger-color);
  font-weight: bold;
}

/* 结算按钮 */
.checkout-actions {
  margin: var(--space-lg) 0;
}

.checkout-actions .el-button {
  width: 100%;
}

/* 购物提示 */
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
  .checkout-content {
    flex-direction: column;
  }

  .checkout-summary {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .section-body {
    padding: var(--space-md);
  }

  .product-item {
    flex-wrap: wrap;
    gap: var(--space-md);
  }

  .product-info {
    order: 1;
    width: 100%;
  }

  .product-quantity {
    order: 2;
  }

  .product-price {
    order: 3;
  }

  .product-subtotal {
    order: 4;
    width: 100%;
    text-align: right;
  }

  .address-card {
    flex-direction: column;
    gap: var(--space-md);
  }

  .address-actions {
    align-self: flex-end;
  }
}
</style>