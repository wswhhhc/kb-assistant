<template>
  <el-tag :type="tagType" size="small" effect="plain">
    {{ statusMap[status] || status }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: { type: String, required: true }
})

const statusMap = {
  UPLOADED: '已上传',
  PARSING: '解析中',
  CHUNKING: '切片中',
  EMBEDDING: '向量化中',
  READY: '已完成',
  FAILED: '失败'
}

const tagType = computed(() => {
  const map = {
    UPLOADED: 'info',
    PARSING: 'warning',
    CHUNKING: 'warning',
    EMBEDDING: 'warning',
    READY: 'success',
    FAILED: 'danger'
  }
  return map[props.status] || 'info'
})
</script>
