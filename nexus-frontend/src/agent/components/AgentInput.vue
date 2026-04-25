<template>
  <div class="agent-input">
    <div class="input-row">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        :placeholder="placeholder"
        :disabled="disabled"
        @keydown.enter.ctrl="submit"
        resize="none"
        class="input-field"
      />
      <el-button
        type="primary"
        :loading="loading"
        :disabled="!inputText.trim() || disabled"
        @click="submit"
        class="send-btn"
      >
        <el-icon v-if="!loading"><Position /></el-icon>
        发送
      </el-button>
    </div>
    <div class="input-tip">按 Ctrl + Enter 快速发送</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Position } from '@element-plus/icons-vue'

const props = defineProps({
  placeholder: {
    type: String,
    default: '请输入您的问题...'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['submit'])

const inputText = ref('')

function submit() {
  if (!inputText.value.trim()) return
  emit('submit', inputText.value)
  inputText.value = ''
}
</script>

<style scoped>
.agent-input {
  padding: 12px 16px;
  background: #fff;
}

.input-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-field {
  flex: 1;
}

.send-btn {
  height: 54px;
  min-width: 80px;
}

.input-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  text-align: right;
}
</style>