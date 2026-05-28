<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <h3 style="margin: 0;">会话历史</h3>
    </div>

    <el-card shadow="never">
      <div style="margin-bottom: 16px;">
        <el-input v-model="searchText" placeholder="搜索会话标题..." clearable style="width: 300px;" @input="handleSearch" />
      </div>

      <el-table :data="filteredSessions" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="title" label="会话标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="知识库" width="160">
          <template #default="{ row }">
            {{ row.kbName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="消息数" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.msgCount ?? '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="最后更新时间" width="170">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goToChat(row.id)">进入会话</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="filteredSessions.length === 0 && !loading" style="text-align: center; padding: 40px; color: #909399;">
        暂无会话记录
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listSessions } from '@/api/chat'

const router = useRouter()
const loading = ref(false)
const sessions = ref([])
const searchText = ref('')

const filteredSessions = computed(() => {
  if (!searchText.value) return sessions.value
  const keyword = searchText.value.toLowerCase()
  return sessions.value.filter(s => (s.title || '').toLowerCase().includes(keyword))
})

function formatTime(t) {
  if (!t) return ''
  return t.substring(0, 19).replace('T', ' ')
}

async function fetchSessions() {
  loading.value = true
  try {
    const res = await listSessions({ pageNum: 1, pageSize: 100 })
    sessions.value = res.data.records || []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  // computed 会自动过滤
}

function goToChat(sessionId) {
  router.push({ path: '/chat', query: { sessionId } })
}

onMounted(fetchSessions)
</script>
