<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-card">
        <!-- Logo -->
        <div class="logo-section">
          <h1>注册账号</h1>
          <p>创建您的Nexus电商账号</p>
        </div>

        <!-- 注册表单 -->
        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
          @submit.prevent="handleRegister"
        >
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="phone">
            <el-input
              v-model="registerForm.phone"
              placeholder="请输入手机号"
              :prefix-icon="Iphone"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="email">
            <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱"
              :prefix-icon="Message"
              size="large"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请确认密码"
              :prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item prop="agree">
            <el-checkbox v-model="registerForm.agree">
              我已阅读并同意
              <router-link to="/agreement" class="agreement-link">《用户协议》</router-link>
              和
              <router-link to="/privacy" class="agreement-link">《隐私政策》</router-link>
            </el-checkbox>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              @click="handleRegister"
              class="register-btn"
            >
              立即注册
            </el-button>
          </el-form-item>

          <div class="login-link">
            已有账号？
            <router-link to="/login">立即登录</router-link>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User,
  Iphone,
  Message,
  Lock
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

// 路由
const router = useRouter()

// 用户store
const userStore = useUserStore()

// 表单引用
const registerFormRef = ref()

// 状态
const registerForm = reactive({
  username: '',
  phone: '',
  email: '',
  password: '',
  confirmPassword: '',
  agree: false
})

const loading = ref(false)

// 表单规则
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const validateAgree = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请同意用户协议和隐私政策'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  agree: [
    { validator: validateAgree, trigger: 'change' }
  ]
}

// 方法
const handleRegister = async () => {
  if (!registerFormRef.value) return

  const valid = await registerFormRef.value.validate()
  if (!valid) return

  loading.value = true

  try {
    // 调用用户store的注册方法
    await userStore.register({
      username: registerForm.username,
      phone: registerForm.phone,
      email: registerForm.email,
      password: registerForm.password,
      status: 1 // 正常状态
    })

    // 注册成功后会自动登录，跳转到首页
    router.push('/')
  } catch (error) {
    // 错误已经在store中处理并显示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--success-color) 100%);
  padding: var(--space-md);
}

.register-container {
  width: 100%;
  max-width: 400px;
}

.register-card {
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

.register-form {
  margin-bottom: var(--space-lg);
}

.agreement-link {
  color: var(--primary-color);
  text-decoration: none;
}

.agreement-link:hover {
  text-decoration: underline;
}

.register-btn {
  width: 100%;
  margin-top: var(--space-sm);
}

.login-link {
  text-align: center;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
  margin-top: var(--space-lg);
}

.login-link a {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>