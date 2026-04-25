<template>
  <div class="main-layout">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="container">
        <div class="flex items-center justify-between">
          <!-- Logo -->
          <router-link to="/" class="logo">
            <span class="logo-text">Nexus电商</span>
          </router-link>

          <!-- 搜索框 -->
          <div class="search-box hidden-md">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品..."
              class="search-input"
              @keyup.enter="handleSearch"
            >
              <template #append>
                <el-button :icon="Search" @click="handleSearch" />
              </template>
            </el-input>
          </div>

          <!-- 右侧菜单 -->
          <div class="nav-menu">
            <div class="flex items-center gap-4">
              <!-- 智能客服 -->
              <el-tooltip content="智能客服" placement="bottom">
                <el-button
                  :icon="Service"
                  circle
                  type="success"
                  @click="$router.push('/customer-service')"
                />
              </el-tooltip>

              <!-- 购物车 -->
              <el-badge :value="cartCount" :max="99" class="cart-badge">
                <el-button
                  :icon="ShoppingCart"
                  circle
                  @click="$router.push('/cart')"
                />
              </el-badge>

              <!-- 用户菜单 -->
              <template v-if="isAuthenticated">
                <el-dropdown @command="handleUserCommand">
                  <div class="user-info">
                    <el-avatar :size="32" :src="userAvatar" />
                    <span class="username">{{ userInfo?.username || '用户' }}</span>
                    <el-icon><ArrowDown /></el-icon>
                  </div>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="profile">
                        <el-icon><User /></el-icon>
                        个人中心
                      </el-dropdown-item>
                      <el-dropdown-item command="orders">
                        <el-icon><Tickets /></el-icon>
                        我的订单
                      </el-dropdown-item>
                      <el-dropdown-item divided command="logout">
                        <el-icon><SwitchButton /></el-icon>
                        退出登录
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
              <template v-else>
                <el-button type="primary" link @click="$router.push('/login')">
                  登录
                </el-button>
                <el-button link @click="$router.push('/register')">
                  注册
                </el-button>
              </template>
            </div>
          </div>
        </div>
      </div>
    </header>

    <!-- 主要内容区域 -->
    <main class="main-content">
      <div class="container">
        <router-view />
      </div>
    </main>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="container">
        <div class="footer-content">
          <div class="footer-section">
            <h3>关于我们</h3>
            <p>Nexus电商系统是一个基于Spring Boot + Vue.js的demo版本，主要用于演示。</p>
          </div>
          <div class="footer-section">
            <h3>联系我们</h3>
            <p>邮箱: 13238740650@163.com</p>
            <p>电话: 132-3874-0650</p>
          </div>
          <div class="footer-section">
            <h3>关注我们</h3>
            <div class="social-links">
              <el-button :icon="Platform" circle />
              <el-button :icon="ChatLineSquare" circle />
              <el-button :icon="VideoCamera" circle />
            </div>
          </div>
        </div>
        <div class="footer-bottom">
          <p>© 2026 Nexus电商系统 版权所有:双创11组 | 备案号: 暂时没有</p>
        </div>
      </div>
    </footer>

    <!-- 悬浮客服按钮 -->
    <div class="float-service" @click="$router.push('/customer-service')">
      <el-icon><Service /></el-icon>
      <span>客服</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'
import {
  Search,
  ShoppingCart,
  Service,
  User,
  Tickets,
  SwitchButton,
  ArrowDown,
  Platform,
  ChatLineSquare,
  VideoCamera
} from '@element-plus/icons-vue'

// 路由
const router = useRouter()
// 用户store
const userStore = useUserStore()
// 购物车store
const cartStore = useCartStore()

// 状态
const searchKeyword = ref('')

// 计算属性
const isAuthenticated = computed(() => userStore.isAuthenticated)
const userInfo = computed(() => userStore.user)
const cartCount = computed(() => cartStore.items.length)

const userAvatar = computed(() => {
  return userInfo.value?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
})

// 方法
const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({
      path: '/products',
      query: { keyword: searchKeyword.value }
    })
  }
}

const handleUserCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'orders':
      router.push('/orders')
      break
    case 'logout':
      handleLogout()
      break
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 使用store的logout方法
    userStore.logout()

    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // 用户取消
  }
}

// 初始化
onMounted(() => {
  // 初始化用户store
  userStore.initialize()

  // 加载购物车数据
  if (isAuthenticated.value) {
    cartStore.loadCart()
  }
})
</script>

<style scoped>
.main-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* 头部样式 */
.header {
  background-color: var(--header-bg-color);
  border-bottom: 1px solid var(--border-color-light);
  padding: var(--space-md) 0;
  position: sticky;
  top: 0;
  z-index: 1000;
  box-shadow: var(--box-shadow-light);
}

.logo {
  text-decoration: none;
  font-size: var(--font-size-xl);
  font-weight: bold;
  color: var(--primary-color);
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.logo-text {
  background: linear-gradient(135deg, var(--primary-color), var(--danger-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.search-box {
  flex: 1;
  max-width: 400px;
  margin: 0 var(--space-lg);
}

.search-input {
  border-radius: var(--border-radius-round);
}

.nav-menu {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  cursor: pointer;
  padding: var(--space-xs);
  border-radius: var(--border-radius);
  transition: var(--transition-all);
}

.user-info:hover {
  background-color: var(--border-color-lighter);
}

.username {
  font-size: var(--font-size-sm);
  color: var(--text-regular);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cart-badge {
  margin-right: var(--space-sm);
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  padding: var(--space-lg) 0;
  background-color: var(--bg-color);
}

/* 页脚样式 */
.footer {
  background-color: var(--sidebar-bg-color);
  color: white;
  padding: var(--space-xl) 0 var(--space-md);
  margin-top: auto;
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: var(--space-xl);
  margin-bottom: var(--space-xl);
}

.footer-section h3 {
  color: white;
  margin-bottom: var(--space-md);
  font-size: var(--font-size-lg);
}

.footer-section p {
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.6;
  margin-bottom: var(--space-xs);
}

.social-links {
  display: flex;
  gap: var(--space-sm);
}

.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding-top: var(--space-md);
  text-align: center;
  color: rgba(255, 255, 255, 0.6);
  font-size: var(--font-size-sm);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-box {
    display: none;
  }

  .header .container > .flex {
    flex-wrap: wrap;
  }

  .logo {
    margin-bottom: var(--space-sm);
  }

  .footer-content {
    grid-template-columns: 1fr;
    gap: var(--space-lg);
  }
}

.gap-4 {
  gap: var(--space-md);
}

/* 悬浮客服按钮 */
.float-service {
  position: fixed;
  right: 20px;
  bottom: 100px;
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #67c23a, #409eff);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
  z-index: 1001;
  color: white;
}

.float-service:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.float-service .el-icon {
  font-size: 20px;
}

.float-service span {
  font-size: 12px;
  margin-top: 2px;
}
</style>