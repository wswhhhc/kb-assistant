<template>
  <div>
    <h3>首页仪表盘</h3>
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="6" v-for="item in stats" :key="item.label">
        <el-card shadow="hover">
          <div style="text-align: center;">
            <div style="font-size: 28px; font-weight: bold; color: #409eff;">{{ item.value }}</div>
            <div style="margin-top: 8px; color: #909399; font-size: 14px;">{{ item.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getStatistics } from '@/api/dashboard'

const stats = ref([
  { label: '用户数', value: 0 },
  { label: '知识库数', value: 0 },
  { label: '文档数', value: 0 },
  { label: '问答次数', value: 0 }
])

onMounted(async () => {
  try {
    const res = await getStatistics()
    if (res.data) {
      stats.value = [
        { label: '用户数', value: res.data.userCount },
        { label: '知识库数', value: res.data.knowledgeBaseCount },
        { label: '文档数', value: res.data.documentCount },
        { label: '问答次数', value: res.data.chatCount }
      ]
    }
  } catch {
    // use default zeros
  }
})
</script>
