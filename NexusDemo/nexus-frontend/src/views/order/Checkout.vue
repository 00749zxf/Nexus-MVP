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

    <div class="checkout-content">
      <!-- 进度条 -->
      <div class="checkout-progress">
        <el-steps :active="currentStep" finish-status="success">
          <el-step title="确认订单" />
          <el-step title="填写信息" />
          <el-step title="选择支付" />
          <el-step title="完成支付" />
        </el-steps>
      </div>

      <!-- 主要内容区 -->
      <div class="checkout-main">
        <div class="checkout-steps">
          <!-- 第一步：确认订单 -->
          <div v-if="currentStep === 0" class="checkout-step">
            <div class="step-header">
              <h3>确认订单信息</h3>
            </div>

            <div class="step-content">
              <!-- 购物车商品列表 -->
              <div class="cart-items">
                <div
                  v-for="item in cartStore.selectedItems"
                  :key="item.id"
                  class="cart-item"
                >
                  <div class="item-image">
                    <img :src="item.image" :alt="item.name" />
                  </div>
                  <div class="item-info">
                    <h4>{{ item.name }}</h4>
                    <div class="item-specs">
                      <span v-for="spec in item.specs" :key="spec.name" class="spec-item">
                        {{ spec.name }}: {{ spec.value }}
                      </span>
                    </div>
                    <div class="item-price">
                      ¥{{ item.price }} × {{ item.quantity }}
                    </div>
                  </div>
                </div>
              </div>

              <!-- 订单摘要 -->
              <div class="order-summary">
                <div class="summary-item">
                  <span class="label">商品合计:</span>
                  <span class="value">¥{{ cartStore.totalAmount }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">优惠:</span>
                  <span class="value discount">-¥{{ cartStore.discountAmount }}</span>
                </div>
                <div class="summary-item">
                  <span class="label">运费:</span>
                  <span class="value">¥{{ shippingFee }}</span>
                </div>
                <div class="summary-total">
                  <span class="label">实付金额:</span>
                  <span class="value total">¥{{ finalAmount }}</span>
                </div>
              </div>

              <!-- 下一步按钮 -->
              <div class="step-actions">
                <el-button type="primary" @click="nextStep">下一步</el-button>
              </div>
            </div>
          </div>

          <!-- 第二步：填写信息 -->
          <div v-if="currentStep === 1" class="checkout-step">
            <div class="step-header">
              <h3>填写收货信息</h3>
            </div>

            <div class="step-content">
              <el-form :model="shippingForm" label-width="120px">
                <el-form-item label="收货人姓名">
                  <el-input v-model="shippingForm.name" />
                </el-form-item>

                <el-form-item label="手机号码">
                  <el-input v-model="shippingForm.phone" />
                </el-form-item>

                <el-form-item label="所在地区">
                  <el-cascader
                    v-model="shippingForm.region"
                    :options="regionOptions"
                    placeholder="请选择地区"
                  />
                </el-form-item>

                <el-form-item label="详细地址">
                  <el-input
                    v-model="shippingForm.address"
                    type="textarea"
                    :rows="3"
                  />
                </el-form-item>

                <el-form-item label="邮政编码">
                  <el-input v-model="shippingForm.postalCode" />
                </el-form-item>

                <el-form-item label="配送方式">
                  <el-radio-group v-model="shippingForm.shippingMethod">
                    <el-radio value="standard">标准配送 (3-5天)</el-radio>
                    <el-radio value="express">快递配送 (1-2天)</el-radio>
                    <el-radio value="overnight">次日达</el-radio>
                  </el-radio-group>
                </el-form-item>

                <el-form-item label="发票信息">
                  <el-checkbox v-model="shippingForm.invoiceNeeded">需要发票</el-checkbox>
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" @click="nextStep">下一步</el-button>
                  <el-button @click="prevStep">上一步</el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>

          <!-- 第三步：选择支付 -->
          <div v-if="currentStep === 2" class="checkout-step">
            <div class="step-header">
              <h3>选择支付方式</h3>
            </div>

            <div class="step-content">
              <el-radio-group v-model="paymentMethod">
                <el-radio value="alipay">
                  <span class="payment-icon">支付宝</span>
                </el-radio>
                <el-radio value="wechat">
                  <span class="payment-icon">微信支付</span>
                </el-radio>
                <el-radio value="bank">
                  <span class="payment-icon">银行卡</span>
                </el-radio>
                <el-radio value="cash">
                  <span class="payment-icon">货到付款</span>
                </el-radio>
              </el-radio-group>

              <div class="step-actions">
                <el-button type="primary" @click="nextStep">下一步</el-button>
                <el-button @click="prevStep">上一步</el-button>
              </div>
            </div>
          </div>

          <!-- 第四步：完成支付 -->
          <div v-if="currentStep === 3" class="checkout-step">
            <div class="step-header">
              <h3>支付处理中...</h3>
            </div>

            <div class="step-content">
              <div class="payment-processing">
                <el-icon class="loading-icon"><Loading /></el-icon>
                <p>正在处理您的支付请求，请稍候...</p>
              </div>

              <div class="step-actions">
                <el-button @click="handlePaymentSuccess">支付成功</el-button>
                <el-button @click="handlePaymentFailure">支付失败</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useCartStore } from '@/store/cart'
import { useAddressStore } from '@/store/address'
import { useUserStore } from '@/store/user'
import { orderApi } from '@/api/order'

// Router & Store
const router = useRouter()
const cartStore = useCartStore()
const addressStore = useAddressStore()
const userStore = useUserStore()

// 状态
const currentStep = ref(0)
const shippingForm = ref({
  name: '',
  phone: '',
  region: [],
  address: '',
  postalCode: '',
  shippingMethod: 'standard',
  invoiceNeeded: false
})
const paymentMethod = ref('alipay')

// 计算属性
const shippingFee = computed(() => {
  return cartStore.totalAmount >= 99 ? 0 : 10
})

const finalAmount = computed(() => {
  return cartStore.totalAmount + shippingFee.value
})

// 地区选项
const regionOptions = ref([
  {
    value: 'beijing',
    label: '北京市',
    children: [
      { value: 'chaoyang', label: '朝阳区' },
      { value: 'haidian', label: '海淀区' },
      { value: 'dongcheng', label: '东城区' }
    ]
  },
  {
    value: 'shanghai',
    label: '上海市',
    children: [
      { value: 'pudong', label: '浦东新区' },
      { value: 'huangpu', label: '黄浦区' },
      { value: 'xuhui', label: '徐汇区' }
    ]
  }
])

// 方法
const nextStep = () => {
  if (currentStep.value < 3) {
    currentStep.value++
  }
}

const prevStep = () => {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}

const handlePaymentSuccess = async () => {
  try {
    // 优先使用addressStore的默认地址
    const defaultAddr = addressStore.defaultAddress

    // 调用API创建订单 - 确保数据完整
    const orderData = {
      receiverName: defaultAddr?.receiverName || shippingForm.value.name || '默认收货人',
      receiverPhone: defaultAddr?.receiverPhone || shippingForm.value.phone || '13800138000',
      receiverAddress: defaultAddr?.fullAddress || shippingForm.value.address || '北京市朝阳区默认地址',
      cartItemIds: cartStore.selectedItems.map(item => item.id)
    }

    console.log('handlePaymentSuccess orderData:', orderData)

    const order = await orderApi.createOrder(orderData)

    ElMessage.success('订单创建成功！')

    // 清空购物车
    await cartStore.clearCart()

    // 跳转到订单详情页
    router.push(`/orders/${order.id}`)
  } catch (error) {
    console.error('创建订单失败:', error)
    ElMessage.error('创建订单失败，请重试')
    currentStep.value = 2
  }
}

const handlePaymentFailure = () => {
  ElMessage.error('支付失败，请重试')
  currentStep.value = 2
}

// 初始化 - 加载地址数据
onMounted(async () => {
  // 检查登录状态
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  // 加载地址数据
  try {
    await addressStore.loadAddresses()
    // 如果有默认地址，填充表单
    if (addressStore.defaultAddress) {
      shippingForm.value.name = addressStore.defaultAddress.receiverName
      shippingForm.value.phone = addressStore.defaultAddress.receiverPhone
      shippingForm.value.address = addressStore.defaultAddress.fullAddress
    }
  } catch (error) {
    console.error('加载地址失败:', error)
  }
})
</script>

<style scoped>
.checkout-page {
  padding-bottom: var(--space-xl);
}

.breadcrumb {
  margin-bottom: var(--space-lg);
}

.checkout-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.checkout-progress {
  margin-bottom: var(--space-lg);
}

.checkout-main {
  display: flex;
  gap: var(--space-lg);
}

.checkout-steps {
  flex: 1;
}

/* 订单确认步骤 */
.cart-items {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  padding: var(--space-lg);
  margin-bottom: var(--space-lg);
}

.cart-item {
  display: flex;
  align-items: center;
  padding: var(--space-md) 0;
  border-bottom: 1px solid var(--border-color-light);
}

.cart-item:last-child {
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

.item-info h4 {
  font-size: var(--font-size-md);
  color: var(--text-primary);
  margin-bottom: var(--space-xs);
}

.item-specs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
  margin-bottom: var(--space-xs);
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.spec-item {
  padding: 2px 8px;
  background-color: var(--bg-color);
  border-radius: var(--border-radius);
}

.item-price {
  font-weight: 500;
  color: var(--danger-color);
}

/* 订单摘要 */
.order-summary {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  padding: var(--space-lg);
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

.summary-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--space-lg);
  padding-top: var(--space-md);
  border-top: 2px solid var(--border-color-light);
}

.summary-total .value {
  font-size: 24px;
  color: var(--danger-color);
  font-weight: bold;
}

/* 支付步骤 */
.checkout-step {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  padding: var(--space-lg);
}

.step-header {
  margin-bottom: var(--space-lg);
}

.step-header h3 {
  font-size: var(--font-size-lg);
  color: var(--text-primary);
}

.step-content {
  margin-bottom: var(--space-lg);
}

.step-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-md);
}

/* 支付处理 */
.payment-processing {
  text-align: center;
  padding: var(--space-xl);
}

.loading-icon {
  font-size: 48px;
  color: var(--primary-color);
  margin-bottom: var(--space-lg);
}

/* 支付方式图标 */
.payment-icon {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .checkout-main {
    flex-direction: column;
  }
}
</style>