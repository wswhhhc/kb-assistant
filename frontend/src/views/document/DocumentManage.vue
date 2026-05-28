<template>
  <div>
    <div style="margin-bottom: 16px;">
      <el-button text @click="$router.push(`/knowledge-bases/${kbId}`)">
        ← 返回知识库详情
      </el-button>
    </div>

    <el-card shadow="never" style="margin-bottom: 20px;" v-if="canManage">
      <template #header>
        <span>上传文档</span>
      </template>
      <el-upload
        drag
        :action="uploadUrl"
        :headers="uploadHeaders"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
        :show-file-list="false"
        accept=".pdf,.docx,.md,.txt"
      >
        <el-icon class="el-icon--upload" :size="48"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或<em>点击上传</em></div>
        <div style="margin-top: 8px; font-size: 13px; color: #606266;">
          当前支持：<strong>PDF / DOCX / MD / TXT</strong>
        </div>
        <template #tip>
          <div class="el-upload__tip" style="color: #909399; margin-top: 8px;">
            支持 PDF、DOCX、MD、TXT 格式，单个文件不超过 50MB
          </div>
        </template>
      </el-upload>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <span>文档列表</span>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip />
        <el-table-column prop="fileType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.fileType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :status="row.parseStatus" />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="上传时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canManage"
              type="primary" link size="small"
              :disabled="row.parseStatus !== 'FAILED'"
              :loading="processingId === row.id"
              @click="handleProcess(row.id)"
            >
              重试
            </el-button>
            <el-popconfirm v-if="canManage" title="确定删除该文档？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { listDocuments, uploadDocument, deleteDocument, processDocument } from '@/api/document'
import { getKnowledgeBase } from '@/api/knowledgeBase'
import { useAuthStore } from '@/stores/auth'
import StatusTag from '@/components/StatusTag.vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'

const route = useRoute()
const authStore = useAuthStore()
const kbId = Number(route.params.kbId)

const kbOwnerId = ref(null)
const loading = ref(false)
const processingId = ref(null)
let pollTimer = null

const canManage = computed(() => {
  return authStore.isAdmin || authStore.userInfo?.id === kbOwnerId.value
})
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const hasProcessingDoc = computed(() => {
  return tableData.value.some(row =>
    ['UPLOADED', 'PARSING', 'CHUNKING', 'EMBEDDING'].includes(row.parseStatus)
  )
})

const uploadUrl = computed(() => `/api/knowledge-bases/${kbId}/documents`)
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function formatTime(t) {
  if (!t) return ''
  return t.substring(0, 19).replace('T', ' ')
}

function beforeUpload(file) {
  const ext = file.name.split('.').pop().toLowerCase()
  const allowed = ['pdf', 'docx', 'md', 'txt']
  if (!allowed.includes(ext)) {
    ElMessage.error('不支持的文件格式')
    return false
  }
  const maxSize = 50 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('文件大小超过 50MB 限制')
    return false
  }
  return true
}

function handleUploadSuccess() {
  ElMessage.success('上传成功，正在处理文档...')
  fetchList()
}

function handleUploadError(err) {
  ElMessage.error('上传失败: ' + (err.message || '未知错误'))
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listDocuments(kbId, { pageNum: pageNum.value, pageSize: pageSize.value })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function handleProcess(id) {
  processingId.value = id
  try {
    const res = await processDocument(id)
    if (res.data?.success) {
      ElMessage.success('文档处理完成')
    } else {
      ElMessage.warning(res.data?.message || '处理失败')
    }
    fetchList()
  } catch {
    ElMessage.error('处理请求失败')
  } finally {
    processingId.value = null
  }
}

async function handleDelete(id) {
  await deleteDocument(id)
  ElMessage.success('删除成功')
  fetchList()
}

async function fetchKb() {
  try {
    const res = await getKnowledgeBase(kbId)
    kbOwnerId.value = res.data.ownerUserId
  } catch {
    kbOwnerId.value = null
  }
}

function startPolling() {
  if (pollTimer) return
  pollTimer = setInterval(() => {
    if (hasProcessingDoc.value) {
      fetchList()
    } else {
      stopPolling()
    }
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(() => {
  fetchKb()
  fetchList()
  startPolling()
})
onUnmounted(() => stopPolling())
</script>
