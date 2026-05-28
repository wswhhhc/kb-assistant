<template>
  <div>
    <div class="page-header">
      <h3 class="page-title">知识库管理</h3>
      <el-button type="primary" @click="openCreateDialog">创建知识库</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
        <el-table-column v-if="authStore.isAdmin" label="创建者" width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="row.ownerUserId === authStore.userInfo?.id ? 'primary' : 'info'" size="small" effect="plain">
              {{ formatOwner(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="可见范围" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.scope === 'PUBLIC' ? 'warning' : ''" size="small" effect="plain">
              {{ row.scope === 'PUBLIC' ? '共享' : '私有' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goDetail(row.id)">详情</el-button>
            <el-button v-if="canManage(row)" type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm v-if="canManage(row)" title="确定删除该知识库？" @confirm="handleDelete(row.id)">
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

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑知识库' : '创建知识库'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入知识库名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入知识库描述" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listKnowledgeBases, createKnowledgeBase, updateKnowledgeBase, deleteKnowledgeBase } from '@/api/knowledgeBase'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref(null)
const form = ref({ name: '', description: '' })

const rules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }]
}

function formatTime(t) {
  if (!t) return ''
  return t.substring(0, 19).replace('T', ' ')
}

function formatOwner(row) {
  if (!row?.ownerUserId) return '-'
  if (row.ownerUserId === authStore.userInfo?.id) return '我创建的'
  return `用户 #${row.ownerUserId}`
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listKnowledgeBases({ pageNum: pageNum.value, pageSize: pageSize.value })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/knowledge-bases/${id}`)
}

function canManage(row) {
  return authStore.isAdmin || authStore.userInfo?.id === row.ownerUserId
}

function openCreateDialog() {
  isEdit.value = false
  editId.value = null
  form.value = { name: '', description: '' }
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.value = { name: row.name, description: row.description || '' }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateKnowledgeBase(editId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await createKnowledgeBase(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  await deleteKnowledgeBase(id)
  ElMessage.success('删除成功')
  fetchList()
}

onMounted(fetchList)
</script>
