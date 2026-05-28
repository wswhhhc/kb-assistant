<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <h3 style="margin: 0;">反馈记录</h3>
    </div>

    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="messageId" label="消息ID" width="100" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column label="反馈类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.feedbackType === 'LIKE' ? 'success' : 'danger'" size="small">
              {{ row.feedbackType === 'LIKE' ? '点赞' : '点踩' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因/备注" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.reason || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="反馈时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>

      <div style="display: flex; justify-content: center; margin-top: 20px;">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchList"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listFeedback } from '@/api/feedback'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

function formatTime(t) {
  if (!t) return ''
  return t.substring(0, 19).replace('T', ' ')
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listFeedback({ pageNum: pageNum.value, pageSize: pageSize.value })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>
