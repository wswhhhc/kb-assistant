<template>
  <el-dialog :model-value="visible" @update:model-value="$emit('update:visible', $event)" title="引用内容" width="600px">
    <div v-if="citation" class="citation-content">
      <div class="citation-meta">
        来源：{{ citation.fileName }} — 片段 {{ citation.chunkIndex }}
        <span v-if="citation.pageNo"> — 第 {{ citation.pageNo }} 页</span>
      </div>
      <div class="citation-text">{{ citation.content }}</div>
      <div class="citation-actions">
        <el-button type="primary" link @click="$emit('go-source', citation)" :disabled="!citation.documentId">
          查看文档原文定位
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  citation: { type: Object, default: null },
})
defineEmits(['update:visible', 'go-source'])
</script>

<style scoped>
.citation-meta {
  margin-bottom: 12px;
  color: #909399;
  font-size: 12px;
}
.citation-text {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-word;
}
.citation-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
</style>
