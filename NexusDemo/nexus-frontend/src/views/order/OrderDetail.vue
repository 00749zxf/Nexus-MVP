<template>
  <div class="order-detail-page">
    <!-- 面包屑导航 -->
    <div class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/orders' }">我的订单</el-breadcrumb-item>
        <el-breadcrumb-item>订单详情</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 加载状态 -->
    <div v-if="orderStore.isLoading" class="loading">
      <el-skeleton :rows="5" animated />
    </div>

    <!-- 订单详情 -->
    <div v-else-if="order" class="order-detail">
      <!-- 订单头部 -->
      <div class="order-header">
        <div class="header-left">
          <h1 class="order-title">订单详情</h1>
          <div class="order-info">
            <span class="order-id">订单号: {{ order.id }}</span>
            <span class="order-time">下单时间: {{ order.createdAt }}</span>
          </div>
        </div>
        <div class="header-right">
          <span class="order-status" :class="`status-${order.status}`">
            {{ order.statusText }}
          </span>
          <span class="order-amount">¥{{ order.totalAmount }}</span>
        </div>
      </div>

      <!-- 订单进度 -->
      <div class="order-progress">
        <el-steps :active="getStepActive(order.status)" align-center>
          <el-step title="待付款" description="等待用户支付" />
          <el-step title="待发货" description="商家准备发货" />
          <el-step title="已发货" description="商品运输中" />
          <el-step title="已完成" description="交易成功" />
        </el-steps>

        <!-- 物流信息 -->
        <div v-if="order.trackingNumber" class="tracking-info">
          <div class="tracking-header">
            <h3>物流信息</h3>
            <el-button type="text" @click="handleTrackOrder">
              查看物流详情
            </el-button>
          </div>
          <div class="tracking-content">
            <div class="tracking-item">
              <span class="label">快递公司:</span>
              <span class="value">顺丰速运</span>
            </div>
            <div class="tracking-item">
              <span class="label">快递单号:</span>
              <span class="value">{{ order.trackingNumber }}</span>
            </div>
            <div class="tracking-item">
              <span class="label">发货时间:</span>
              <span class="value">{{ order.shippedAt }}</span>
            </div>
            <div class="tracking-item">
              <span class="label">最新状态:</span>
              <span class="value">已发货，运输中</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 订单内容 -->
      <div class="order-content">
        <!-- 收货地址 -->
        <div class="address-card">
          <div class="card-header">
            <h3 class="card-title">
              <el-icon><Location /></el-icon>
              收货地址
            </h3>
          </div>
          <div class="card-body">
            <div class="address-info">
              <div class="address-detail">
                <div class="receiver">
                  <span class="name">{{ order.shippingAddress.name }}</span>
                  <span class="phone">{{ order.shippingAddress.phone }}</span>
                </div>
                <div class="address">
                  {{ order.shippingAddress.province }}
                  {{ order.shippingAddress.city }}
                  {{ order.shippingAddress.district }}
                  {{ order.shippingAddress.detail }}
                </div>
                <div class="postal-code">邮编: {{ order.shippingAddress.postalCode }}</div>
              </div>
              <div class="address-actions">
                <el-button type="text" @click="handleChangeAddress">
                  修改地址
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 商品信息 -->
        <div class="products-card">
          <div class="card-header">
            <h3 class="card-title">
              <el-icon><ShoppingCart /></el-icon>
              商品信息
            </h3>
          </div>
          <div class="card-body">
            <div class="products-list">
              <div
                v-for="item in order.items"
                :key="item.productId"
                class="product-item"
              >
                <div class="product-image">
                  <img :src="item.image" :alt="item.name" />
                </div>
                <div class="product-info">
                  <h4 class="product-name">{{ item.name }}</h4>
                  <div class="product-specs">
                    <span
                      v-for="spec in item.specs"
                      :key="spec.name"
                      class="spec-item"
                    >
                      {{ spec.name }}: {{ spec.value }}
                    </span>
                  </div>
                  <div class="product-price">¥{{ item.price }}</div>
                </div>
                <div class="product-quantity">×{{ item.quantity }}</div>
                <div class="product-subtotal">
                  <div class="subtotal">小计: ¥{{ item.price * item.quantity }}</div>
                  <div
                    v-if="order.status === 'completed'"
                    class="product-actions"
                  >
                    <el-button type="text" @click="handleViewProduct(item)">
                      查看商品
                    </el-button>
                    <el-button type="text" @click="handleAddReview(item)">
                      评价商品
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 订单摘要 -->
        <div class="summary-card">
          <div class="card-header">
            <h3 class="card-title">
              <el-icon><Document /></el-icon>
              订单摘要
            </h3>
          </div>
          <div class="card-body">
            <div class="summary-list">
              <div class="summary-item">
                <span class="label">商品总价</span>
                <span class="value">¥{{ order.totalAmount - order.shippingFee + order.discountAmount }}</span>
              </div>
              <div class="summary-item" v-if="order.discountAmount > 0">
                <span class="label">优惠减免</span>
                <span class="value discount">-¥{{ order.discountAmount }}</span>
              </div>
              <div class="summary-item">
                <span class="label">运费</span>
                <span class="value">
                  <span v-if="order.shippingFee === 0">免运费</span>
                  <span v-else>¥{{ order.shippingFee }}</span>
                </span>
              </div>
              <div class="summary-item">
                <span class="label">支付方式</span>
                <span class="value">{{ getPaymentMethodText(order.paymentMethod) }}</span>
              </div>
              <div class="summary-item">
                <span class="label">配送方式</span>
                <span class="value">{{ getShippingMethodText(order.shippingMethod) }}</span>
              </div>
              <div class="summary-divider"></div>
              <div class="summary-total">
                <span class="label">实付金额</span>
                <span class="value">¥{{ order.totalAmount }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 订单操作 -->
      <div class="order-actions">
        <div class="actions-left">
          <el-button
            v-if="order.status === 'pending'"
            type="primary"
            size="large"
            @click="handlePay"
          >
            立即支付
          </el-button>
          <el-button
            v-if="order.status === 'pending'"
            size="large"
            @click="handleCancel"
          >
            取消订单
          </el-button>
          <el-button
            v-if="order.status === 'shipped'"
            type="primary"
            size="large"
            @click="handleConfirmReceipt"
          >
            确认收货
          </el-button>
          <el-button
            v-if="order.status === 'completed'"
            size="large"
            @click="handleBuyAgain"
          >
            再次购买
          </el-button>
        </div>
        <div class="actions-right">
          <el-button size="large" @click="$router.push('/orders')">
            返回订单列表
          </el-button>
          <el-button
            v-if="['completed', 'cancelled'].includes(order.status)"
            size="large"
            @click="handleDelete"
          >
            删除订单
          </el-button>
        </div>
      </div>
    </div>

    <!-- 订单不存在 -->
    <div v-else class="order-not-found">
      <el-empty description="订单不存在或已被删除">
        <template #image>
          <el-icon :size="100"><Warning /></el-icon>
        </template>
        <el-button type="primary" @click="$router.push('/orders')">
          返回订单列表
        </el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Location,
  ShoppingCart,
  Document,
  Warning
} from '@element-plus/icons-vue'
import { useOrderStore } from '@/store/order'
import { useUserStore } from '@/store/user'

// 路由
const route = useRoute()
const router = useRouter()

// Store
const orderStore = useOrderStore()
const userStore = useUserStore()

// 状态
const orderId = ref(route.params.id)

// 计算属性
const order = computed(() => orderStore.currentOrder)

// 方法
const getStepActive = (status) => {
  const steps = {
    pending: 0,
    paid: 1,
    shipped: 2,
    completed: 3,
    cancelled: -1
  }
  return steps[status] || -1
}

const getPaymentMethodText = (method) => {
  const methods = {
    alipay: '支付宝',
    wechat: '微信支付',
    bank: '银行卡',
    cash: '货到付款'
  }
  return methods[method] || method
}

const getShippingMethodText = (method) => {
  const methods = {
    standard: '标准配送',
    express: '快递配送',
    overnight: '次日达'
  }
  return methods[method] || method
}

const handleTrackOrder = () => {
  ElMessage.info('物流详情功能开发中')
}

const handleChangeAddress = () => {
  ElMessage.info('修改地址功能开发中')
}

const handleViewProduct = (item) => {
  router.push(`/products/${item.productId}`)
}

const handleAddReview = (item) => {
  ElMessage.info('评价商品功能开发中')
}

const handlePay = async () => {
  try {
    await ElMessageBox.confirm('确认要支付该订单吗？', '支付确认', {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // TODO: 调用支付API
    order.value.status = 'paid'
    order.value.statusText = '待发货'
    ElMessage.success('支付成功')
  } catch {
    // 用户取消
  }
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确认要取消该订单吗？', '取消订单', {
      confirmButtonText: '确认取消',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderStore.cancelOrder(orderId.value)
    ElMessage.success('订单已取消')
  } catch (error) {
    console.error('取消订单失败:', error)
  }
}

const handleConfirmReceipt = async () => {
  try {
    await ElMessageBox.confirm('确认收到商品了吗？', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderStore.confirmReceipt(orderId.value)
    ElMessage.success('确认收货成功')
  } catch (error) {
    console.error('确认收货失败:', error)
  }
}

const handleBuyAgain = async () => {
  try {
    await ElMessageBox.confirm('确认要再次购买这些商品吗？', '再次购买', {
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })

    const { useCartStore } = await import('@/store/cart')
    const cartStore = useCartStore()

    for (const item of order.value.items) {
      const product = {
        id: item.productId,
        name: item.name,
        image: item.image,
        price: item.price,
        stock: 100
      }
      await cartStore.addToCart(product, item.quantity, item.specs)
    }

    router.push('/cart')
  } catch {
    // 用户取消
  }
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确认要删除该订单吗？', '删除订单', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // TODO: 调用API删除订单
    orderStore.orders = orderStore.orders.filter(o => o.id !== orderId.value)
    ElMessage.success('订单已删除')
    router.push('/orders')
  } catch {
    // 用户取消
  }
}

// 生命周期
onMounted(async () => {
  // 检查用户是否登录
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    await orderStore.getOrderById(orderId.value)
  } catch (error) {
    console.error('加载订单详情失败:', error)
  }
})
</script>

<style scoped>
.order-detail-page {
  padding-bottom: var(--space-xl);
}

/* 订单头部 */
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: var(--space-lg);
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  margin-bottom: var(--space-lg);
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.order-title {
  font-size: 24px;
  color: var(--text-primary);
  margin: 0;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.order-id {
  font-weight: 500;
  color: var(--text-primary);
}

.order-time {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-md);
}

.order-status {
  padding: 6px 16px;
  border-radius: var(--border-radius);
  font-size: var(--font-size-md);
  font-weight: 500;
}

.status-pending {
  background-color: rgba(255, 153, 0, 0.1);
  color: #ff9900;
  border: 1px solid rgba(255, 153, 0, 0.2);
}

.status-paid {
  background-color: rgba(64, 158, 255, 0.1);
  color: var(--primary-color);
  border: 1px solid rgba(64, 158, 255, 0.2);
}

.status-shipped {
  background-color: rgba(103, 194, 58, 0.1);
  color: var(--success-color);
  border: 1px solid rgba(103, 194, 58, 0.2);
}

.status-completed {
  background-color: rgba(144, 147, 153, 0.1);
  color: var(--text-secondary);
  border: 1px solid rgba(144, 147, 153, 0.2);
}

.status-cancelled {
  background-color: rgba(245, 108, 108, 0.1);
  color: var(--danger-color);
  border: 1px solid rgba(245, 108, 108, 0.2);
}

.order-amount {
  font-size: 28px;
  color: var(--danger-color);
  font-weight: bold;
}

/* 订单进度 */
.order-progress {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  padding: var(--space-lg);
  margin-bottom: var(--space-lg);
}

.tracking-info {
  margin-top: var(--space-lg);
  padding-top: var(--space-lg);
  border-top: 1px solid var(--border-color-light);
}

.tracking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-md);
}

.tracking-header h3 {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin: 0;
}

.tracking-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-md);
}

.tracking-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.tracking-item .label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.tracking-item .value {
  font-weight: 500;
  color: var(--text-primary);
}

/* 订单内容 */
.order-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: var(--space-lg);
  margin-bottom: var(--space-lg);
}

.address-card,
.products-card,
.summary-card {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  overflow: hidden;
}

.card-header {
  padding: var(--space-md) var(--space-lg);
  background-color: var(--bg-color);
  border-bottom: 1px solid var(--border-color-light);
}

.card-title {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin: 0;
}

.card-body {
  padding: var(--space-lg);
}

/* 地址信息 */
.address-info {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.address-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.receiver {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
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

.postal-code {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

/* 商品信息 */
.products-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.product-item {
  display: flex;
  align-items: flex-start;
  padding: var(--space-md);
  border: 1px solid var(--border-color-light);
  border-radius: var(--border-radius);
}

.product-image {
  width: 80px;
  height: 80px;
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
  margin-bottom: var(--space-xs);
}

.spec-item {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  padding: 2px 8px;
  background-color: var(--bg-color);
  border-radius: var(--border-radius);
}

.product-price {
  font-size: var(--font-size-md);
  color: var(--danger-color);
  font-weight: bold;
}

.product-quantity {
  width: 60px;
  text-align: center;
  color: var(--text-regular);
  margin: 0 var(--space-md);
}

.product-subtotal {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: var(--space-sm);
}

.product-subtotal .subtotal {
  font-weight: 500;
  color: var(--text-primary);
}

.product-actions {
  display: flex;
  gap: var(--space-sm);
}

/* 订单摘要 */
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

.summary-total .label {
  font-size: var(--font-size-md);
  color: var(--text-primary);
}

.summary-total .value {
  font-size: 24px;
  color: var(--danger-color);
  font-weight: bold;
}

/* 订单操作 */
.order-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-lg);
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
}

.actions-left,
.actions-right {
  display: flex;
  gap: var(--space-md);
}

/* 加载状态 */
.loading {
  padding: var(--space-xl);
}

/* 订单不存在 */
.order-not-found {
  text-align: center;
  padding: var(--space-xl);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .order-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-md);
  }

  .header-right {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }

  .order-progress {
    padding: var(--space-md);
  }

  .order-content {
    grid-template-columns: 1fr;
  }

  .address-info {
    flex-direction: column;
    gap: var(--space-md);
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

  .product-subtotal {
    order: 3;
    width: 100%;
    align-items: flex-end;
  }

  .order-actions {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-md);
  }

  .actions-left,
  .actions-right {
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
  }
}
</style>