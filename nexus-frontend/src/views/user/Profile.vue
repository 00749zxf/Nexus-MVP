<template>
  <div class="profile-page">
    <!-- 面包屑导航 -->
    <div class="breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>个人中心</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="6" animated />
    </div>

    <!-- 正常内容 -->
    <div v-else class="profile-content">
      <!-- 用户信息卡片 -->
      <div class="profile-card">
        <div class="card-header">
          <h2>个人信息</h2>
          <el-button type="primary" @click="handleEditProfile">编辑资料</el-button>
        </div>

        <div class="card-body">
          <div class="profile-info">
            <div class="avatar-section">
              <el-avatar :size="100" :src="userAvatar" />
              <el-button type="primary" size="small" @click="showAvatarDialog = true">
                更换头像
              </el-button>
            </div>

            <div class="info-details">
              <div class="info-item">
                <span class="label">用户名:</span>
                <span class="value">{{ userInfo.username }}</span>
              </div>
              <div class="info-item">
                <span class="label">邮箱:</span>
                <span class="value">{{ userInfo.email || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">手机:</span>
                <span class="value">{{ userInfo.phone || '未设置' }}</span>
              </div>
              <div class="info-item">
                <span class="label">注册时间:</span>
                <span class="value">{{ userInfo.createTime || '未知' }}</span>
              </div>
              <div class="info-item">
                <span class="label">账户状态:</span>
                <span class="value">
                  <el-tag :type="userInfo.status === 1 ? 'success' : 'danger'">
                    {{ userInfo.status === 1 ? '正常' : '禁用' }}
                  </el-tag>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 订单统计 -->
      <div class="order-stats">
        <h3>订单统计</h3>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-value">{{ orderStats.total }}</div>
            <div class="stat-label">总订单数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ orderStats.pending }}</div>
            <div class="stat-label">待付款</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ orderStats.paid }}</div>
            <div class="stat-label">待发货</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ orderStats.shipped }}</div>
            <div class="stat-label">待收货</div>
          </div>
        </div>
      </div>

      <!-- 收货地址管理 -->
      <div class="address-section">
        <div class="section-header">
          <h3>收货地址</h3>
          <el-button type="primary" @click="handleAddAddress">
            <el-icon><Plus /></el-icon>
            新增地址
          </el-button>
        </div>

        <div v-if="addressStore.hasAddresses" class="address-list">
          <div
            v-for="address in addressStore.addresses"
            :key="address.id"
            class="address-card"
            :class="{ 'is-default': address.isDefault }"
          >
            <div class="address-info">
              <div class="address-header">
                <span class="receiver-name">{{ address.receiverName }}</span>
                <span class="receiver-phone">{{ address.receiverPhone }}</span>
                <el-tag v-if="address.isDefault" type="success" size="small">默认</el-tag>
              </div>
              <div class="address-detail">
                {{ address.fullAddress }}
              </div>
            </div>

            <div class="address-actions">
              <el-button
                v-if="!address.isDefault"
                type="text"
                @click="handleSetDefaultAddress(address.id)"
              >
                设为默认
              </el-button>
              <el-button type="text" @click="handleEditAddress(address)">
                编辑
              </el-button>
              <el-button type="text" @click="handleDeleteAddress(address.id)">
                删除
              </el-button>
            </div>
          </div>
        </div>

        <el-empty v-else description="暂无收货地址">
          <el-button type="primary" @click="handleAddAddress">添加地址</el-button>
        </el-empty>
      </div>

      <!-- 快捷操作 -->
      <div class="quick-actions">
        <h3>快捷操作</h3>
        <div class="actions-grid">
          <el-button type="primary" @click="$router.push('/orders')">
            查看所有订单
          </el-button>
          <el-button type="primary" @click="$router.push('/cart')">
            查看购物车
          </el-button>
          <el-button type="primary" @click="$router.push('/favorites')">
            我的收藏
          </el-button>
          <el-button type="warning" @click="showPasswordDialog = true">
            修改密码
          </el-button>
          <el-button type="danger" @click="handleLogout">
            退出登录
          </el-button>
        </div>
      </div>
    </div>

    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="400px"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="passwordForm.currentPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 编辑资料对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑资料"
      width="500px"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="editForm.phone" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 更换头像对话框 -->
    <el-dialog
      v-model="showAvatarDialog"
      title="更换头像"
      width="400px"
    >
      <div class="avatar-upload">
        <div class="avatar-preview">
          <el-avatar :size="120" :src="avatarPreviewUrl" />
        </div>

        <div class="avatar-input">
          <el-form-item label="头像URL">
            <el-input
              v-model="avatarUrl"
              placeholder="请输入头像图片URL"
              @change="updateAvatarPreview"
            />
          </el-form-item>
        </div>

        <div class="preset-avatars">
          <p class="preset-label">或选择预设头像:</p>
          <div class="preset-list">
            <el-avatar
              v-for="(url, index) in presetAvatars"
              :key="index"
              :size="50"
              :src="url"
              class="preset-avatar"
              @click="selectPresetAvatar(url)"
            />
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showAvatarDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAvatar">保存</el-button>
      </template>
    </el-dialog>

    <!-- 地址编辑对话框 -->
    <el-dialog
      v-model="showAddressDialog"
      :title="isEditingAddress ? '编辑地址' : '新增地址'"
      width="500px"
    >
      <el-form
        ref="addressFormRef"
        :model="addressForm"
        :rules="addressRules"
        label-width="100px"
      >
        <el-form-item label="收货人姓名" prop="receiverName">
          <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="收货人电话" prop="receiverPhone">
          <el-input v-model="addressForm.receiverPhone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="addressForm.province" placeholder="如：北京市" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="addressForm.city" placeholder="如：北京市" />
        </el-form-item>
        <el-form-item label="区/县" prop="district">
          <el-input v-model="addressForm.district" placeholder="如：朝阳区" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="addressForm.detailAddress"
            type="textarea"
            :rows="3"
            placeholder="请输入详细地址"
          />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-checkbox v-model="addressForm.isDefault">设为默认收货地址</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddressDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useOrderStore } from '@/store/order'
import { useAddressStore } from '@/store/address'

// 路由
const router = useRouter()

// Store
const userStore = useUserStore()
const orderStore = useOrderStore()
const addressStore = useAddressStore()

// 用户信息 - 从store获取
const userInfo = computed(() => userStore.user || {})

const userAvatar = computed(() => {
  return userInfo.value.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
})

// 订单统计
const orderStats = reactive({
  total: 0,
  pending: 0,
  paid: 0,
  shipped: 0
})

// 编辑对话框
const showEditDialog = ref(false)
const editFormRef = ref()
const editForm = reactive({
  username: '',
  email: '',
  phone: ''
})

const editRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 头像对话框
const showAvatarDialog = ref(false)
const avatarUrl = ref('')
const avatarPreviewUrl = computed(() => {
  return avatarUrl.value || userAvatar.value
})

// 预设头像
const presetAvatars = [
  'https://cube.elemecdn.com/0/88/03b0d39583f482067fc0679b65f5mpng.png',
  'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png',
  'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
  'https://cube.elemecdn.com/e/fd/0fc6d4490b9f60f8d8bfac3e.png'
]

// 地址对话框
const showAddressDialog = ref(false)
const addressFormRef = ref()
const isEditingAddress = ref(false)
const editingAddressId = ref(null)
const addressForm = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false
})

const addressRules = {
  receiverName: [
    { required: true, message: '请输入收货人姓名', trigger: 'blur' }
  ],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  province: [
    { required: true, message: '请输入省份', trigger: 'blur' }
  ],
  city: [
    { required: true, message: '请输入城市', trigger: 'blur' }
  ],
  district: [
    { required: true, message: '请输入区/县', trigger: 'blur' }
  ],
  detailAddress: [
    { required: true, message: '请输入详细地址', trigger: 'blur' }
  ]
}

// 密码修改对话框
const showPasswordDialog = ref(false)
const passwordFormRef = ref()
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 方法
const handleEditProfile = () => {
  editForm.username = userInfo.value.username || ''
  editForm.email = userInfo.value.email || ''
  editForm.phone = userInfo.value.phone || ''
  showEditDialog.value = true
}

const handleSaveProfile = async () => {
  if (!editFormRef.value) return

  const valid = await editFormRef.value.validate()
  if (!valid) return

  try {
    await userStore.updateProfile({
      username: editForm.username,
      email: editForm.email,
      phone: editForm.phone
    })
    showEditDialog.value = false
  } catch (error) {
    // 错误已在store中处理
  }
}

// 头像相关
const updateAvatarPreview = () => {
  // 预览URL已经通过computed自动更新
}

const selectPresetAvatar = (url) => {
  avatarUrl.value = url
}

const handleSaveAvatar = async () => {
  if (!avatarUrl.value) {
    ElMessage.warning('请选择或输入头像')
    return
  }

  try {
    await userStore.updateProfile({ avatar: avatarUrl.value })
    showAvatarDialog.value = false
    avatarUrl.value = ''
    ElMessage.success('头像已更新')
  } catch (error) {
    ElMessage.error('更新头像失败')
  }
}

// 地址相关
const handleAddAddress = () => {
  isEditingAddress.value = false
  editingAddressId.value = null
  Object.assign(addressForm, {
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    isDefault: false
  })
  showAddressDialog.value = true
}

const handleEditAddress = (address) => {
  isEditingAddress.value = true
  editingAddressId.value = address.id
  Object.assign(addressForm, {
    receiverName: address.receiverName,
    receiverPhone: address.receiverPhone,
    province: address.province,
    city: address.city,
    district: address.district,
    detailAddress: address.detailAddress,
    isDefault: address.isDefault
  })
  showAddressDialog.value = true
}

const handleSaveAddress = async () => {
  if (!addressFormRef.value) return

  const valid = await addressFormRef.value.validate()
  if (!valid) return

  try {
    if (isEditingAddress.value) {
      await addressStore.updateAddress(editingAddressId.value, addressForm)
    } else {
      await addressStore.addAddress(addressForm)
    }
    showAddressDialog.value = false
  } catch (error) {
    // 错误已在store中处理
  }
}

const handleDeleteAddress = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个地址吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await addressStore.deleteAddress(id)
  } catch {
    // 用户取消
  }
}

const handleSetDefaultAddress = async (id) => {
  try {
    await addressStore.setDefaultAddress(id)
  } catch (error) {
    // 错误已在store中处理
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    userStore.logout()
    router.push('/login')
  } catch {
    // 用户取消
  }
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordFormRef.value) return

  const valid = await passwordFormRef.value.validate()
  if (!valid) return

  try {
    await userStore.updateProfile({ password: passwordForm.newPassword })
    showPasswordDialog.value = false
    // 清空密码表单
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error) {
    ElMessage.error('修改密码失败')
  }
}

// 加载订单统计
const loadOrderStats = async () => {
  try {
    // 获取各状态的订单数量（后端状态码：0=待付款, 1=待发货, 2=已发货, 3=已完成, 4=已取消）
    orderStats.total = await orderStore.getOrderCount() || 0
    orderStats.pending = await orderStore.getOrderCount(0) || 0
    orderStats.paid = await orderStore.getOrderCount(1) || 0
    orderStats.shipped = await orderStore.getOrderCount(2) || 0
  } catch (error) {
    console.error('获取订单统计失败:', error)
  }
}

// 加载状态
const loading = ref(true)

// 初始化
onMounted(async () => {
  // 检查登录状态
  if (!userStore.isAuthenticated) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  // 并行加载所有数据
  const loadPromises = []

  if (!userStore.user) {
    loadPromises.push(userStore.loadUser())
  }

  loadPromises.push(loadOrderStats())
  loadPromises.push(addressStore.loadAddresses())

  await Promise.all(loadPromises)
  loading.value = false
})
</script>

<style scoped>
.profile-page {
  padding-bottom: var(--space-xl);
}

.breadcrumb {
  margin-bottom: var(--space-lg);
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

/* 用户信息卡片 */
.profile-card {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  padding: var(--space-lg);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-lg);
}

.card-header h2 {
  font-size: var(--font-size-lg);
  color: var(--text-primary);
}

.profile-info {
  display: flex;
  gap: var(--space-lg);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
}

.info-details {
  flex: 1;
}

.info-item {
  display: flex;
  margin-bottom: var(--space-md);
}

.info-item .label {
  width: 80px;
  color: var(--text-secondary);
}

.info-item .value {
  color: var(--text-primary);
  font-weight: 500;
}

/* 订单统计 */
.order-stats {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  padding: var(--space-lg);
}

.order-stats h3 {
  font-size: var(--font-size-lg);
  color: var(--text-primary);
  margin-bottom: var(--space-lg);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: var(--space-md);
}

.stat-item {
  text-align: center;
  padding: var(--space-md);
  background-color: var(--bg-color);
  border-radius: var(--border-radius);
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--primary-color);
  margin-bottom: var(--space-xs);
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

/* 地址管理 */
.address-section {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  padding: var(--space-lg);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-lg);
}

.section-header h3 {
  font-size: var(--font-size-lg);
  color: var(--text-primary);
}

.address-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--space-md);
}

.address-card {
  background-color: var(--bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  padding: var(--space-md);
  transition: border-color 0.3s;
}

.address-card.is-default {
  border-color: var(--success-color);
}

.address-info {
  margin-bottom: var(--space-md);
}

.address-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
}

.receiver-name {
  font-weight: 500;
  color: var(--text-primary);
}

.receiver-phone {
  color: var(--text-secondary);
}

.address-detail {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.5;
}

.address-actions {
  display: flex;
  gap: var(--space-sm);
}

/* 快捷操作 */
.quick-actions {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color-light);
  padding: var(--space-lg);
}

.quick-actions h3 {
  font-size: var(--font-size-lg);
  color: var(--text-primary);
  margin-bottom: var(--space-lg);
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-md);
}

.actions-grid .el-button {
  width: 100%;
}

/* 头像上传 */
.avatar-upload {
  text-align: center;
}

.avatar-preview {
  margin-bottom: var(--space-lg);
}

.avatar-input {
  margin-bottom: var(--space-lg);
}

.preset-avatars {
  margin-top: var(--space-md);
}

.preset-label {
  color: var(--text-secondary);
  margin-bottom: var(--space-sm);
}

.preset-list {
  display: flex;
  gap: var(--space-sm);
  justify-content: center;
}

.preset-avatar {
  cursor: pointer;
  transition: transform 0.2s;
}

.preset-avatar:hover {
  transform: scale(1.1);
}

/* 响应式 */
@media (max-width: 768px) {
  .profile-info {
    flex-direction: column;
  }

  .address-list {
    grid-template-columns: 1fr;
  }
}

/* 加载状态 */
.loading-state {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  padding: var(--space-xl);
}
</style>