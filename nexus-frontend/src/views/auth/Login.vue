<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-card">
        <!-- Logo -->
        <div class="logo-section">
          <h1>Nexus电商</h1>
          <p>欢迎回来，请登录您的账号</p>
        </div>

        <!-- 登录表单 -->
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
              autocomplete="current-password"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
              <router-link to="/forgot-password" class="forgot-link">
                忘记密码？
              </router-link>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="userStore.loading"
              @click="handleLogin"
              class="login-btn"
            >
              登录
            </el-button>
          </el-form-item>

          <div class="register-link">
            还没有账号？
            <router-link to="/register">立即注册</router-link>
          </div>
        </el-form>

        <!-- 测试账号提示 -->
        <div class="test-accounts">
          <p>测试账号：</p>
          <p>用户名: user / 密码: user123</p>
          <p>用户名: admin2 / 密码: admin123</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

// 路由
const router = useRouter()
const route = useRoute()

// 用户store
const userStore = useUserStore()

// 表单引用
const loginFormRef = ref()

// 状态
const loginForm = reactive({
  username: '',
  password: ''
})

const rememberMe = ref(false)

// 表单规则
const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
}

// 方法
const handleLogin = async () => {
  if (!loginFormRef.value) return

  const valid = await loginFormRef.value.validate()
  if (!valid) return

  try {
    await userStore.login(loginForm)

    // 跳转到目标页面或首页
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (error) {
    // 错误已经在store中处理并显示
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--danger-color) 100%);
  padding: var(--space-md);
}

.login-container {
  width: 100%;
  max-width: 400px;
}

.login-card {
  background-color: var(--card-bg-color);
  border-radius: var(--border-radius);
  padding: var(--space-xl);
  box-shadow: var(--box-shadow);
}

.logo-section {
  text-align: center;
  margin-bottom: var(--space-xl);
}

.logo-section h1 {
  font-size: var(--font-size-xl);
  color: var(--primary-color);
  margin-bottom: var(--space-xs);
}

.logo-section p {
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.login-form {
  margin-bottom: var(--space-lg);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.forgot-link {
  color: var(--primary-color);
  text-decoration: none;
  font-size: var(--font-size-sm);
}

.forgot-link:hover {
  text-decoration: underline;
}

.login-btn {
  width: 100%;
  margin-top: var(--space-sm);
}

.register-link {
  text-align: center;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  margin-top: var(--space-lg);
}

.register-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.register-link a:hover {
  text-decoration: underline;
}

.social-login {
  margin-top: var(--space-xl);
}

.divider {
  display: flex;
  align-items: center;
  text-align: center;
  margin: var(--space-lg) 0;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid var(--border-color);
}

.divider span {
  padding: 0 var(--space-md);
}

.social-buttons {
  display: flex;
  justify-content: center;
  gap: var(--space-md);
}

.test-accounts {
  margin-top: var(--space-lg);
  padding: var(--space-md);
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: var(--border-radius);
}

.test-accounts p {
  color: #fff;
  margin: 4px 0;
  font-size: var(--font-size-sm);
}

.test-accounts p:first-child {
  margin-bottom: var(--space-xs);
}
</style>