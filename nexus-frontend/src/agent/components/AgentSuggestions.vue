<template>
  <div class="agent-suggestions" v-if="items && items.length > 0">
    <div class="suggestions-title">您可以：</div>
    <div class="suggestions-list">
      <el-button
        v-for="item in items"
        :key="item.text"
        size="small"
        :type="item.type === 'LINK' ? 'primary' : 'default'"
        @click="handleClick(item)"
      >
        {{ item.text }}
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['select'])
const router = useRouter()

function handleClick(item) {
  if (item.type === 'LINK' && item.url) {
    router.push(item.url)
  } else if (item.type === 'ACTION') {
    emit('select', item)
  }
}
</script>

<style scoped>
.agent-suggestions {
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-top: 8px;
}

.suggestions-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.suggestions-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.suggestions-list .el-button {
  margin: 0;
}
</style>