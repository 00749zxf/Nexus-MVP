import { createRouter, createWebHistory } from 'vue-router'

// 布局组件（稍后创建）
import MainLayout from '@/layouts/MainLayout.vue'

// 页面组件（使用懒加载）
const Login = () => import('@/views/auth/Login.vue')
const Register = () => import('@/views/auth/Register.vue')
const Home = () => import('@/views/home/Home.vue')
const ProductList = () => import('@/views/product/ProductList.vue')
const ProductDetail = () => import('@/views/product/ProductDetail.vue')
const Cart = () => import('@/views/cart/Cart.vue')
const OrderList = () => import('@/views/order/OrderList.vue')
const OrderDetail = () => import('@/views/order/OrderDetail.vue')
const Checkout = () => import('@/views/order/Checkout.vue')
const Profile = () => import('@/views/user/Profile.vue')
const Favorites = () => import('@/views/favorite/Favorites.vue')
const NotFound = () => import('@/views/error/NotFound.vue')

// 路由配置
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresGuest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { requiresGuest: true }
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: Home,
        meta: { title: '首页' }
      },
      {
        path: 'products',
        name: 'ProductList',
        component: ProductList,
        meta: { title: '商品列表' }
      },
      {
        path: 'products/:id',
        name: 'ProductDetail',
        component: ProductDetail,
        meta: { title: '商品详情' }
      },
      {
        path: 'cart',
        name: 'Cart',
        component: Cart,
        meta: { title: '购物车', requiresAuth: true }
      },
      {
        path: 'checkout',
        name: 'Checkout',
        component: Checkout,
        meta: { title: '结算', requiresAuth: true }
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: OrderList,
        meta: { title: '我的订单', requiresAuth: true }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: OrderDetail,
        meta: { title: '订单详情', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: Profile,
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: Favorites,
        meta: { title: '我的收藏', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
    meta: { title: '页面未找到' }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 路由守卫 - 身份验证
router.beforeEach((to, from, next) => {
  // 设置页面标题
  const title = to.meta.title || 'Nexus电商系统'
  document.title = `${title} - Nexus电商系统`

  // 获取用户登录状态（检查 token 是否存在且非空）
  const token = localStorage.getItem('token')
  const isAuthenticated = token && token.length > 0

  // 检查是否需要登录
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!isAuthenticated) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
    } else {
      next()
    }
  } else if (to.matched.some(record => record.meta.requiresGuest)) {
    if (isAuthenticated) {
      next({ name: 'Home' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router