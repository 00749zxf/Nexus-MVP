<template>
  <div class="customer-service-page">
    <!-- 顶部标题栏 -->
    <div class="header">
      <div class="header-title">
        <el-icon><Service /></el-icon>
        <span>Nexus智能客服</span>
      </div>
      <el-button type="default" size="small" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        退出客服
      </el-button>
    </div>

    <!-- 主体区域 -->
    <div class="main-content">
      <!-- 左侧：Agent对话 -->
      <div class="chat-panel">
        <AgentChat
          type="CUSTOMER_SERVICE"
          :context="chatContext"
          @suggestion-select="onSuggestionSelect"
        />
      </div>

      <!-- 右侧：快速入口 -->
      <div class="side-panel">
        <div class="quick-actions">
          <div class="section-title">快速咨询</div>
          <el-button v-for="item in quickQuestions" :key="item" text @click="askQuickQuestion(item)">
            {{ item }}
          </el-button>

          <div class="section-divider"></div>

          <div class="section-title">常见问题</div>
          <div class="faq-list">
            <div v-for="faq in faqList" :key="faq.id" class="faq-item" @click="askQuickQuestion(faq.question)">
              <el-icon><QuestionFilled /></el-icon>
              <span>{{ faq.question }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useAgentStore } from '@/agent/store/agent'
import AgentChat from '@/agent/components/AgentChat.vue'
import { QuestionFilled, Service, ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const agentStore = useAgentStore()

// 对话上下文
const chatContext = computed(() => ({
  memberId: userStore.user?.id,
  username: userStore.user?.username,
  currentPage: router.currentRoute.value.path
}))

// 快速问题
const quickQuestions = ref([
  '查询我的订单',
  '商品发货时间',
  '如何退款',
  '修改收货地址'
])

// FAQ列表
const faqList = ref([
  { id: 1, question: '如何申请退款？' },
  { id: 2, question: '订单什么时候发货？' },
  { id: 3, question: '支持哪些支付方式？' },
  { id: 4, question: '退换货政策是什么？' }
])

// 退出客服
function goBack() {
  agentStore.clearSession()
  router.back()
}

// 快速提问
function askQuickQuestion(question) {
  agentStore.sendMessage(question, chatContext.value)
}

// 建议操作选择
function onSuggestionSelect(item) {
  console.log('Suggestion selected:', item)
}
</script>

<style scoped>
.customer-service-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.header-title .el-icon {
  color: #409eff;
}

.main-content {
  flex: 1;
  display: flex;
  padding: 16px;
  gap: 16px;
  overflow: hidden;
}

.chat-panel {
  flex: 1;
  min-width: 0;
}

.side-panel {
  width: 280px;
  flex-shrink: 0;
}

.quick-actions {
  height: 100%;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  overflow-y: auto;
}

.section-title {
  font-weight: 500;
  color: #303133;
  margin-bottom: 12px;
}

.quick-actions .el-button {
  display: block;
  width: 100%;
  margin-bottom: 8px;
  text-align: left;
}

.section-divider {
  height: 1px;
  background: #e4e7ed;
  margin: 16px 0;
}

.faq-list {
  margin-top: 8px;
}

.faq-item {
  display: flex;
  align-items: center;
  padding: 8px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
}

.faq-item:hover {
  background: #e9e9eb;
}

.faq-item .el-icon {
  margin-right: 8px;
  color: #409eff;
}

.faq-item span {
  color: #606266;
  font-size: 13px;
}
</style>