<template>
  <div>
    <div v-if="message.role === 'USER'" class="message-row user-row">
      <div class="message-bubble user-bubble">{{ message.content }}</div>
    </div>
    <div v-else class="message-row ai-row">
      <div class="message-bubble ai-bubble">
        <!-- 思考过程 -->
        <div v-if="message.thinkingContent" class="thinking-section">
          <div class="thinking-header" @click="message._showThinking = !message._showThinking">
            <span class="thinking-icon">✦</span>
            <span>{{ message._showThinking ? '收起思考过程' : '展开思考过程' }}</span>
            <span class="thinking-toggle">{{ message._showThinking ? '▲' : '▼' }}</span>
          </div>
          <div v-show="message._showThinking" class="thinking-body">
            {{ message.thinkingContent }}
          </div>
        </div>

        <!-- AI 回答 -->
        <div class="ai-content" v-html="renderMarkdown(message.content, message.citations, msgIdx)"></div>

        <!-- 反馈按钮 -->
        <div v-if="message.id" class="feedback-actions">
          <el-button size="small" text
            :type="message.feedbackType === 'LIKE' ? 'success' : 'default'"
            :loading="message._feedbackLoading"
            @click="$emit('submit-feedback', { type: 'LIKE' })">
            <el-icon><CircleCheck /></el-icon>
            <span class="feedback-label">有帮助</span>
          </el-button>
          <el-button size="small" text
            :type="message.feedbackType === 'DISLIKE' ? 'danger' : 'default'"
            :loading="message._feedbackLoading"
            @click="$emit('open-dislike')">
            <el-icon><CircleClose /></el-icon>
            <span class="feedback-label">需要改进</span>
          </el-button>
        </div>

        <!-- 引用来源 -->
        <div v-if="message.citations && message.citations.length > 0" class="citation-section">
          <el-divider class="citation-divider" />
          <div class="citation-header" @click="$emit('toggle-citation')">
            <span class="citation-title">引用来源（{{ message.citations.length }} 条）</span>
            <el-icon :class="{ rotated: message._showCitations }" class="citation-arrow">
              <ArrowDown />
            </el-icon>
          </div>
          <template v-if="message._showCitations">
            <div v-for="(c, ci) in message.citations" :key="ci"
              class="citation-item"
              :id="'citation-ref-' + msgIdx + '-' + ci"
              :class="{ 'citation-highlighted': message._highlightedIdx === ci }"
              @click="$emit('open-citation', c)">
              <el-tag size="small" type="info" class="citation-tag">{{ ci + 1 }}</el-tag>
              <span class="citation-file">{{ c.fileName }} — 片段 {{ c.chunkIndex }}</span>
              <span v-if="c.pageNo" class="citation-page">第 {{ c.pageNo }} 页</span>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ArrowDown, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { renderMarkdown } from '@/utils/markdown'

defineProps({
  message: { type: Object, required: true },
  msgIdx: { type: Number, required: true },
})

defineEmits(['toggle-citation', 'open-citation', 'submit-feedback', 'open-dislike'])
</script>

<style scoped>
.message-row {
  display: flex;
}
.user-row { justify-content: flex-end; }
.ai-row { justify-content: flex-start; }
.message-bubble {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
}
.user-bubble {
  background: #409eff;
  color: #fff;
}
.ai-bubble {
  background: #f5f7fa;
  color: #303133;
}
.ai-content { word-break: break-word; }

.feedback-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.feedback-label {
  margin-left: 4px;
}

.citation-section { margin-top: 4px; }
.citation-divider { margin: 8px 0; }
.citation-header {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 0;
  user-select: none;
}
.citation-header:hover { color: #409eff; }
.citation-title { font-size: 12px; color: #909399; }
.citation-arrow { transition: transform 0.2s; }
.rotated { transform: rotate(180deg); }
.citation-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 6px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
}
.citation-item:hover { background: #e8edf3; }
.citation-highlighted { background: #ecf5ff !important; border-left: 3px solid #409eff; }
.citation-tag { margin-right: 4px; cursor: pointer; }
.citation-file { font-size: 12px; cursor: pointer; }
.citation-page { font-size: 12px; color: #909399; margin-left: auto; }

:deep(.citation-marker) {
  color: #409eff;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  padding: 0 1px;
}
:deep(.citation-marker:hover) {
  color: #1a6bb5;
  background: #ecf5ff;
  border-radius: 2px;
}

.thinking-section {
  background: #f0f2f5;
  border-radius: 8px;
  margin-bottom: 12px;
  border-left: 3px solid #409eff;
}
.thinking-header {
  padding: 6px 12px;
  cursor: pointer;
  user-select: none;
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 6px;
}
.thinking-header:hover { color: #409eff; }
.thinking-icon { font-size: 12px; color: #409eff; }
.thinking-toggle { margin-left: auto; font-size: 10px; }
.thinking-body {
  padding: 0 12px 10px;
  font-size: 13px;
  color: #909399;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
