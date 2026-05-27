<template>
  <div class="citation-panel" v-if="citations && citations.length > 0">
    <h4 style="margin: 12px 0 8px; font-size: 14px;">引用来源</h4>
    <div v-for="(item, index) in citations" :key="index" class="citation-item">
      <el-tag size="small" type="info" class="citation-index">{{ index + 1 }}</el-tag>
      <span class="citation-text">{{ item.fileName }} — 片段 {{ item.chunkIndex }}</span>
      <span v-if="item.pageNo" class="citation-page">第 {{ item.pageNo }} 页</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  citations: { type: Array, default: () => [] }
})
</script>

<style scoped>
.citation-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 13px;
  color: #606266;
}
.citation-index {
  flex-shrink: 0;
}
.citation-page {
  color: #909399;
  margin-left: auto;
}
</style>
