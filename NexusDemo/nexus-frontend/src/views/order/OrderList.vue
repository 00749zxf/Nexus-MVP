<template>
  <div class="order-page">
    <!-- 面包屑导航 -->
    <div class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>我的订单</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">我的订单</h1>
      <div class="page-subtitle">查看和管理您的所有订单</div>
    </div>

    <!-- 订单筛选 -->
    <div class="order-filter">
      <div class="filter-tabs">
        <el-tabs v-model="activeStatus" @tab-change="handleStatusChange">
          <el-tab-pane label="全部" name="all" />
          <el-tab-pane label="待付款" name="pending" />
          <el-tab-pane label="待发货" name="paid" />
          <el-tab-pane label="已发货" name="shipped" />
          <el-tab-pane label="已完成" name="completed" />
          <el-tab-pane label="已取消" name="cancelled" />
        </el-tabs>
      </div>

      <div class="filter-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索订单号或商品名称"
          clearable
          style="width: 300px"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" :icon="Search" @click="handleSearch">
          搜索
        </el-button>
      </div>
    </div>

    <!-- 订单列表 -->
    <div class="order-list">
      <el-empty
        v-if="orderStore.orders.length === 0 && !orderStore.isLoading"
        description="暂无订单"
      >
        <template #image>
          <el-icon :size="100"><Document /></el-icon>
        </template>
        <el-button type="primary" @click="$router.push('/products')">
          去购物
        </el-button>
      </el-empty>

      <div v-else>
        <!-- 加载状态 -->
        <div v-if="orderStore.isLoading" class="loading">
          <el-skeleton :rows="3" animated />
        </div>

        <!-- 订单卡片 -->
        <div
          v-for="order in filteredOrders"
          :key="order.id"
          class="order-card"
        >
          <!-- 订单头部 -->
          <div class="order-header">
            <div class="header-left">
              <span class="order-id">订单号: {{ order.id }}</span>
              <span class="order-time">下单时间: {{ order.createdAt }}</span>
            </div>
            <div class="header-right">
              <span class="order-status" :class="`status-${order.status}`">
                {{ order.statusText }}
              </span>
              <span class="order-amount">¥{{ order.totalAmount }}</span>
            </div>
          </div>

          <!-- 订单商品 -->
          <div class="order-items">
            <div
              v-for="item in order.items"
              :key="item.productId"
              class="order-item"
            >
              <div class="item-image">
                <img :src="item.image" :alt="item.name" />
              </div>
              <div class="item-info">
                <h3 class="item-name">{{ item.name }}</h3>
                <div class="item-specs">
                  <span
                    v-for="spec in item.specs"
                    :key="spec.name"
                    class="spec-item"
                  >
                    {{ spec.name }}: {{ spec.value }}
                  </span>
                </div>
                <div class="item-price">¥{{ item.price }}</div>
              </div>
              <div class="item-quantity">×{{ item.quantity }}</div>
              <div class="item-subtotal">小计: ¥{{ item.price * item.quantity }}</div>
            </div>
          </div>

          <!-- 订单摘要 -->
          <div class="order-summary">
            <div class="summary-left">
              <div class="summary-item">
                <span class="label">商品件数:</span>
                <span class="value">{{ order.itemCount }}件</span>
              </div>
              <div class="summary-item">
                <span class="label">运费:</span>
                <span class="value">
                  <span v-if="order.shippingFee === 0">免运费</span>
                  <span v-else>¥{{ order.shippingFee }}</span>
                </span>
              </div>
              <div class="summary-item" v-if="order.discountAmount > 0">
                <span class="label">优惠:</span>
                <span class="value discount">-¥{{ order.discountAmount }}</span>
              </div>
            </div>
            <div class="summary-right">
              <div class="total-amount">
                <span class="label">实付金额:</span>
                <span class="value">¥{{ order.totalAmount }}</span>
              </div>
            </div>
          </div>

          <!-- 订单操作 -->
          <div class="order-actions">
            <div class="actions-left">
              <span class="shipping-info" v-if="order.trackingNumber">
                快递单号: {{ order.trackingNumber }}
              </span>
            </div>
            <div class="actions-right">
              <el-button
                v-if="order.status === 'pending'"
                type="primary"
                size="small"
                @click="handlePay(order)"
              >
                立即支付
              </el-button>
              <el-button
                v-if="order.status === 'pending'"
                size="small"
                @click="handleCancel(order)"
              >
                取消订单
              </el-button>
              <el-button
                v-if="order.status === 'shipped'"
                type="primary"
                size="small"
                @click="handleConfirmReceipt(order)"
              >
                确认收货
              </el-button>
              <el-button
                size="small"
                @click="$router.push(`/orders/${order.id}`)"
              >
                查看详情
              </el-button>
              <el-button
                v-if="['completed', 'cancelled'].includes(order.status)"
                size="small"
                @click="handleDelete(order)"
              >
                删除订单
              </el-button>
              <el-button
                v-if="order.status === 'completed'"
                size="small"
                @click="handleBuyAgain(order)"
              >
                再次购买
              </el-button>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="orderStore.orders.length > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="totalOrders"
            :page-sizes="[5, 10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Document } from '@element-plus/icons-vue'
import { useOrderStore } from '@/store/order'
import { useUserStore } from '@/store/user'

// 路由
const router = useRouter()

// Store
const orderStore = useOrderStore()
const userStore = useUserStore()

// 状态
const activeStatus = ref('all')
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

// 计算属性
const totalOrders = computed(() => orderStore.orders.length)

// 状态映射
const statusMap = {
  'pending': 0,
  'paid': 1,
  'shipped': 2,
  'completed': 3,
  'cancelled': 4
}

const filteredOrders = computed(() => {
  let filtered = orderStore.orders

  // 按状态筛选（前端订单status是数字）
  if (activeStatus.value !== 'all' && statusMap[activeStatus.value] !== undefined) {
    const numericStatus = statusMap[activeStatus.value]
    filtered = filtered.filter(order => order.status === numericStatus)
  }

  // 按关键字搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(order =>
      order.id.toLowerCase().includes(keyword) ||
      order.items.some(item => item.name.toLowerCase().includes(keyword))
    )
  }

  // 分页
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filtered.slice(start, end)
})

// 方法
const handleStatusChange = () => {
  currentPage.value = 1
  loadOrders()
}

const handleSearch = () => {
  currentPage.value = 1
  // 这里可以调用API搜索，目前使用本地过滤
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
}

const handleCurrentChange = (page) => {
  currentPage.value = page
}

const handlePay = async (order) => {
  try {
    await ElMessageBox.confirm('确认要支付该订单吗？', '支付确认', {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // TODO: 调用支付API
    order.status = 'paid'
    order.statusText = '待发货'
    ElMessage.success('支付成功')
  } catch {
    // 用户取消
  }
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm('确认要取消该订单吗？', '取消订单', {
      confirmButtonText: '确认取消',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderStore.cancelOrder(order.id)
    ElMessage.success('订单已取消')
  } catch (error) {
    console.error('取消订单失败:', error)
  }
}

const handleConfirmReceipt = async (order) => {
  try {
    await ElMessageBox.confirm('确认收到商品了吗？', '确认收货', {
      confirmButtonText: '确认收货',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderStore.confirmReceipt(order.id)
    ElMessage.success('确认收货成功')
  } catch (error) {
    console.error('确认收货失败:', error)
  }
}

const handleDelete = async (order) => {
  try {
    await ElMessageBox.confirm('确认要删除该订单吗？', '删除订单', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await orderStore.deleteOrder(order.id)
    ElMessage.success('订单已删除')
  } catch {
    // 用户取消
  }
}

const handleBuyAgain = async (order) => {
  try {
    await ElMessageBox.confirm('确认要再次购买这些商品吗？', '再次购买', {
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })

    // TODO: 将商品加入购物车
    const { useCartStore } = await import('@/store/cart')
    const cartStore = useCartStore()

    for (const item of order.items) {
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

const loadOrders = async () => {
  try {
    // 状态映射已在上方定义：'all' 不传参数，其他转换为数字
    const params = {}
    if (activeStatus.value !== 'all' && statusMap[activeStatus.value] !== undefined) {
      params.status = statusMap[activeStatus.value]
    }

    await orderStore.loadOrders(params)
  } catch (error) {
    console.error('加载订单失败:', error)
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

  await loadOrders()
})
</script>

<style scoped>
.order-page {
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

/* 订单筛选 */
.order-filter {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  padding: var(--space-md);
  margin-bottom: var(--space-lg);
}

.filter-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--space-md);
}

/* 订单卡片 */
.order-card {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  margin-bottom: var(--space-lg);
  overflow: hidden;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md);
  background-color: var(--bg-color);
  border-bottom: 1px solid var(--border-color-light);
}

.header-left {
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
  align-items: center;
  gap: var(--space-lg);
}

.order-status {
  padding: 4px 12px;
  border-radius: var(--border-radius);
  font-size: var(--font-size-sm);
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
  font-size: 18px;
  color: var(--danger-color);
  font-weight: bold;
}

/* 订单商品 */
.order-items {
  padding: var(--space-md);
}

.order-item {
  display: flex;
  align-items: center;
  padding: var(--space-md);
  border-bottom: 1px solid var(--border-color-light);
}

.order-item:last-child {
  border-bottom: none;
}

.item-image {
  width: 80px;
  height: 80px;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  overflow: hidden;
  margin-right: var(--space-md);
  flex-shrink: 0;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
}

.item-specs {
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

.item-price {
  font-size: var(--font-size-md);
  color: var(--danger-color);
  font-weight: bold;
}

.item-quantity {
  width: 60px;
  text-align: center;
  color: var(--text-regular);
}

.item-subtotal {
  width: 120px;
  text-align: right;
  font-weight: 500;
  color: var(--text-primary);
}

/* 订单摘要 */
.order-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md);
  background-color: var(--bg-color);
  border-top: 1px solid var(--border-color-light);
}

.summary-left {
  display: flex;
  gap: var(--space-lg);
}

.summary-item {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
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

.summary-right {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}

.total-amount {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.total-amount .label {
  font-size: var(--font-size-md);
  color: var(--text-primary);
}

.total-amount .value {
  font-size: 24px;
  color: var(--danger-color);
  font-weight: bold;
}

/* 订单操作 */
.order-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-md);
  border-top: 1px solid var(--border-color-light);
}

.shipping-info {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.actions-right {
  display: flex;
  gap: var(--space-sm);
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  margin-top: var(--space-lg);
  padding: var(--space-md);
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
}

/* 加载状态 */
.loading {
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
    justify-content: space-between;
  }

  .order-item {
    flex-wrap: wrap;
    gap: var(--space-md);
  }

  .item-info {
    order: 1;
    width: 100%;
  }

  .item-quantity {
    order: 2;
  }

  .item-subtotal {
    order: 3;
    margin-left: auto;
  }

  .order-summary {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-md);
  }

  .summary-left {
    flex-direction: column;
    gap: var(--space-sm);
  }

  .summary-right {
    width: 100%;
    justify-content: flex-end;
  }

  .order-actions {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-md);
  }

  .actions-right {
    width: 100%;
    justify-content: flex-end;
    flex-wrap: wrap;
  }
}
</style>