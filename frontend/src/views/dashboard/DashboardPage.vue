<template>
  <div>
    <div style="margin-bottom: 20px;">
      <h3 style="margin: 0 0 8px 0;">{{ pageTitle }}</h3>
      <div style="color: #909399; font-size: 14px;">{{ pageDescription }}</div>
    </div>

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
import { computed, ref, onMounted } from 'vue'
import { getStatistics } from '@/api/dashboard'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const dashboardData = ref(null)

const adminStatsTemplate = [
  { label: '用户数', field: 'userCount' },
  { label: '知识库', field: 'knowledgeBaseCount' },
  { label: '文档总数', field: 'documentCount' },
  { label: '已就绪文档', field: 'readyDocumentCount' },
  { label: '问答次数', field: 'chatCount' },
  { label: '反馈数量', field: 'feedbackCount' },
  { label: '失败问题', field: 'failedQuestionCount' }
]

const userStatsTemplate = [
  { label: '我的知识库', field: 'knowledgeBaseCount' },
  { label: '我上传的文档', field: 'documentCount' },
  { label: '已就绪文档', field: 'readyDocumentCount' },
  { label: '我的问答次数', field: 'chatCount' },
  { label: '我的反馈数', field: 'feedbackCount' },
  { label: '我的失败问题', field: 'failedQuestionCount' }
]

const isAdminView = computed(() => {
  if (dashboardData.value?.adminView !== undefined) {
    return !!dashboardData.value.adminView
  }
  return !!authStore.isAdmin
})

const pageTitle = computed(() => (isAdminView.value ? '系统仪表盘' : '个人工作台'))
const pageDescription = computed(() => (
  isAdminView.value
    ? '查看平台整体使用情况、知识库规模与问答质量统计。'
    : '查看你自己的知识库使用情况、上传进度和问答记录。'
))

const stats = computed(() => {
  const template = isAdminView.value ? adminStatsTemplate : userStatsTemplate
  const data = dashboardData.value || {}
  return template.map(item => ({
    label: item.label,
    value: data[item.field] ?? '-'
  }))
})

onMounted(async () => {
  try {
    const res = await getStatistics()
    dashboardData.value = res.data || {}
  } catch {
    dashboardData.value = {}
  }
})
</script>
