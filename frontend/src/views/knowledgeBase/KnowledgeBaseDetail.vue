<template>
  <div>
    <div style="margin-bottom: 16px;">
      <el-button text @click="$router.push('/knowledge-bases')">
        ← 返回知识库列表
      </el-button>
    </div>

    <el-row :gutter="20">
      <!-- 基本信息 -->
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>
                基本信息
                <el-tag size="small" :type="kb.scope === 'PUBLIC' ? 'warning' : ''" effect="plain" style="margin-left: 8px;">
                  {{ kb.scope === 'PUBLIC' ? '共享' : '私有' }}
                </el-tag>
              </span>
              <div>
                <el-button type="primary" size="small" @click="goDocuments">文档管理</el-button>
                <el-button size="small" @click="isEditing = true" v-if="!isEditing && canManage">编辑</el-button>
              </div>
            </div>
          </template>

          <template v-if="!isEditing">
            <div style="margin-bottom: 12px;">
              <label style="color: #909399; font-size: 13px;">名称</label>
              <div style="margin-top: 4px;">{{ kb.name }}</div>
            </div>
            <div style="margin-bottom: 12px;">
              <label style="color: #909399; font-size: 13px;">描述</label>
              <div style="margin-top: 4px;">{{ kb.description || '暂无描述' }}</div>
            </div>
            <div style="margin-bottom: 12px;">
              <label style="color: #909399; font-size: 13px;">状态</label>
              <div style="margin-top: 4px;">
                <el-tag :type="kb.status === 'ACTIVE' ? 'success' : 'info'" size="small">
                  {{ kb.status === 'ACTIVE' ? '启用' : '停用' }}
                </el-tag>
              </div>
            </div>
            <div>
              <label style="color: #909399; font-size: 13px;">创建时间</label>
              <div style="margin-top: 4px;">{{ formatTime(kb.createdAt) }}</div>
            </div>
          </template>

          <template v-else>
            <el-form :model="editForm" label-width="60px">
              <el-form-item label="名称">
                <el-input v-model="editForm.name" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="editForm.description" type="textarea" :rows="3" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="small" @click="handleSaveEdit" :loading="saving">保存</el-button>
                <el-button size="small" @click="isEditing = false">取消</el-button>
              </el-form-item>
            </el-form>
          </template>
        </el-card>
      </el-col>

      <!-- 成员管理 -->
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span>成员管理</span>
              <el-button type="primary" size="small" @click="showAddMember = true" v-if="authStore.isAdmin">添加成员</el-button>
            </div>
          </template>

          <el-table :data="memberList" v-loading="memberLoading" stripe size="small">
            <el-table-column prop="userId" label="用户 ID" width="80" />
            <el-table-column label="角色" width="80">
              <template #default="{ row }">
                <el-tag size="small" type="warning" v-if="row.userId === ownerId">拥有者</el-tag>
                <el-tag size="small" type="info" v-else>成员</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" v-if="authStore.isAdmin">
              <template #default="{ row }">
                <el-popconfirm
                  v-if="row.userId !== ownerId"
                  title="确定移除该成员？"
                  @confirm="handleRemoveMember(row.userId)"
                >
                  <template #reference>
                    <el-button type="danger" link size="small">移除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加成员对话框 -->
    <el-dialog v-model="showAddMember" title="添加成员" width="400px">
      <el-form label-width="80px">
        <el-form-item label="用户 ID">
          <el-input-number v-model="newMemberUserId" :min="1" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddMember = false">取消</el-button>
        <el-button type="primary" @click="handleAddMember" :loading="addingMember">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getKnowledgeBase, updateKnowledgeBase } from '@/api/knowledgeBase'
import { listMembers, addMember, removeMember } from '@/api/member'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const kbId = Number(route.params.id)

const kb = ref({})
const ownerId = ref(null)
const loading = ref(false)
const isEditing = ref(false)
const saving = ref(false)
const editForm = ref({ name: '', description: '' })

const memberList = ref([])
const memberLoading = ref(false)
const showAddMember = ref(false)
const newMemberUserId = ref(null)
const addingMember = ref(false)

const canManage = computed(() => {
  return authStore.isAdmin || authStore.userInfo?.id === ownerId.value
})

function formatTime(t) {
  if (!t) return ''
  return t.substring(0, 19).replace('T', ' ')
}

async function fetchKb() {
  loading.value = true
  try {
    const res = await getKnowledgeBase(kbId)
    kb.value = res.data
    ownerId.value = res.data.ownerUserId
    editForm.value = { name: res.data.name, description: res.data.description || '' }
  } finally {
    loading.value = false
  }
}

async function fetchMembers() {
  memberLoading.value = true
  try {
    const res = await listMembers(kbId)
    memberList.value = res.data || []
  } finally {
    memberLoading.value = false
  }
}

async function handleSaveEdit() {
  saving.value = true
  try {
    await updateKnowledgeBase(kbId, editForm.value)
    ElMessage.success('更新成功')
    isEditing.value = false
    fetchKb()
  } finally {
    saving.value = false
  }
}

async function handleAddMember() {
  if (!newMemberUserId.value) {
    ElMessage.warning('请输入用户 ID')
    return
  }
  addingMember.value = true
  try {
    await addMember(kbId, { userId: newMemberUserId.value })
    ElMessage.success('添加成功')
    showAddMember.value = false
    newMemberUserId.value = null
    fetchMembers()
  } finally {
    addingMember.value = false
  }
}

async function handleRemoveMember(userId) {
  await removeMember(kbId, userId)
  ElMessage.success('移除成功')
  fetchMembers()
}

function goDocuments() {
  router.push(`/documents/${kbId}`)
}

onMounted(() => {
  fetchKb()
  fetchMembers()
})
</script>
