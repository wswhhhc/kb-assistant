<template>
  <el-scrollbar ref="scrollbarRef" class="message-area">
    <div v-for="(msg, idx) in messages" :key="idx" class="message-wrapper">
      <ChatMessageBubble
        :message="msg"
        :msg-idx="idx"
        @toggle-citation="$emit('toggle-citation', idx)"
        @open-citation="$emit('open-citation', $event)"
        @submit-feedback="$emit('submit-feedback', msg, $event.type, $event.reason, $event.reasonType)"
        @open-dislike="$emit('open-dislike', msg)"
      />
    </div>
    <div v-if="messages.length === 0 && !loading" class="empty-state">
      <el-icon :size="48" class="empty-icon"><ChatDotRound /></el-icon>
      <p class="empty-hint">输入问题开始问答</p>
    </div>
  </el-scrollbar>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { ChatDotRound } from '@element-plus/icons-vue'
import ChatMessageBubble from './ChatMessageBubble.vue'

const props = defineProps({
  messages: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
})

defineEmits(['toggle-citation', 'open-citation', 'submit-feedback', 'open-dislike'])

const scrollbarRef = ref(null)

function scrollToBottom() {
  nextTick(() => {
    if (scrollbarRef.value?.wrapRef) {
      scrollbarRef.value.wrapRef.scrollTop = scrollbarRef.value.wrapRef.scrollHeight
    }
  })
}

// 监听新消息自动滚动
watch(() => props.messages.length, () => scrollToBottom())

// 监听引用导航事件
onMounted(() => {
  window.addEventListener('citation-navigate', (e) => {
    const { msgIdx, ci } = e.detail
    nextTick(() => {
      const el = document.getElementById('citation-ref-' + msgIdx + '-' + ci)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
        el.classList.add('citation-flash')
        setTimeout(() => el.classList.remove('citation-flash'), 1500)
      }
    })
  })
})

defineExpose({ scrollToBottom })
</script>

<style scoped>
.message-area {
  flex: 1;
  padding: 20px;
}
.message-wrapper {
  margin-bottom: 16px;
}
.empty-state {
  text-align: center;
  color: #909399;
  margin-top: 120px;
}
.empty-icon {
  color: #dcdfe6;
}
.empty-hint {
  margin-top: 12px;
  font-size: 14px;
}
:deep(.citation-flash) {
  animation: citationFlash 1.5s ease-out;
}
@keyframes citationFlash {
  0% { background: #ecf5ff; }
  50% { background: #b3d8ff; }
  100% { background: #ecf5ff; }
}
</style>
