<template>
  <div>
    <div style="margin-bottom: 16px;">
      <el-button text @click="$router.push(`/documents/${kbId}`)">
        ← 返回文档列表
      </el-button>
    </div>

    <el-card shadow="never" style="margin-bottom: 16px;">
      <template #header>
        <span>文档信息</span>
      </template>
      <div v-if="documentInfo">
        <div style="margin-bottom: 10px;">
          <label style="color: #909399; font-size: 13px;">文件名</label>
          <div style="margin-top: 4px;">{{ documentInfo.fileName }}</div>
        </div>
        <div style="display: flex; gap: 24px; flex-wrap: wrap;">
          <div>
            <label style="color: #909399; font-size: 13px;">状态</label>
            <div style="margin-top: 4px;">{{ documentInfo.parseStatus }}</div>
          </div>
          <div>
            <label style="color: #909399; font-size: 13px;">页数</label>
            <div style="margin-top: 4px;">{{ documentInfo.pageCount || '-' }}</div>
          </div>
          <div>
            <label style="color: #909399; font-size: 13px;">切片数</label>
            <div style="margin-top: 4px;">{{ documentInfo.chunkCount || chunks.length }}</div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>文档原文切片</span>
          <span style="font-size: 12px; color: #909399;">支持从问答引用跳转定位</span>
        </div>
      </template>

      <div v-if="loading" style="padding: 24px; color: #909399;">加载中...</div>
      <div v-else-if="chunks.length === 0" style="padding: 24px; color: #909399;">暂无可展示切片</div>

      <div v-else>
        <div
          v-for="chunk in chunks"
          :key="chunk.id"
          :id="`chunk-${chunk.chunkIndex}`"
          class="chunk-card"
          :class="{ highlighted: highlightChunkIndex === chunk.chunkIndex }"
        >
          <div class="chunk-header">
            <div>
              <el-tag size="small" type="info">片段 {{ chunk.chunkIndex }}</el-tag>
              <el-tag v-if="chunk.pageNo" size="small" style="margin-left: 8px;">第 {{ chunk.pageNo }} 页</el-tag>
            </div>
            <span style="font-size: 12px; color: #909399;">{{ chunk.charCount || 0 }} 字</span>
          </div>
          <div v-if="chunk.sectionTitle" class="chunk-title">{{ chunk.sectionTitle }}</div>
          <div class="chunk-content">{{ chunk.content }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getDocumentDetail } from '@/api/document'

const route = useRoute()
const kbId = Number(route.params.kbId)
const documentId = Number(route.params.documentId)
const highlightChunkIndex = ref(Number(route.query.chunkIndex || NaN))
const loading = ref(false)
const documentInfo = ref(null)
const chunks = ref([])

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getDocumentDetail(documentId)
    documentInfo.value = res.data.document || null
    chunks.value = res.data.chunks || []
    await nextTick()
    scrollToChunk()
  } finally {
    loading.value = false
  }
}

function scrollToChunk() {
  if (!Number.isFinite(highlightChunkIndex.value)) return
  const el = document.getElementById(`chunk-${highlightChunkIndex.value}`)
  if (!el) return
  el.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

onMounted(fetchDetail)
</script>

<style scoped>
.chunk-card {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 14px;
  background: #fff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.chunk-card.highlighted {
  border-color: #409eff;
  background: #f0f7ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.08);
}

.chunk-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.chunk-title {
  font-weight: 600;
  margin-bottom: 8px;
}

.chunk-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  color: #303133;
}
</style>
