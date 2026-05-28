<template>
  <div>
    <div class="page-header">
      <h3 class="page-title">用户管理</h3>
      <el-button type="primary" @click="showCreate = true">创建用户</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="姓名" width="150" />
        <el-table-column prop="email" label="邮箱" min-width="200" />
        <el-table-column label="角色" width="80">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'ACTIVE'"
              :disabled="row.id === currentUserId"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openPasswordDialog(row)">重置密码</el-button>
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

    <!-- 创建用户对话框 -->
    <el-dialog v-model="showCreate" title="创建用户" width="450px">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" maxlength="50" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password maxlength="100" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="createForm.realName" maxlength="50" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="createForm.email" maxlength="100" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role" style="width: 100%;">
            <el-option label="用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="showPassword" title="重置密码" width="400px">
      <el-form label-width="80px">
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPassword = false">取消</el-button>
        <el-button type="primary" @click="handleResetPassword" :loading="resetting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listUsers, createUser, updateUserStatus, resetUserPassword } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

const authStore = useAuthStore()
const currentUserId = authStore.userInfo?.id

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const showCreate = ref(false)
const creating = ref(false)
const createFormRef = ref(null)
const createForm = ref({ username: '', password: '', realName: '', email: '', role: 'USER' })
const createRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const showPassword = ref(false)
const resetUserId = ref(null)
const newPassword = ref('')
const resetting = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const res = await listUsers({ pageNum: pageNum.value, pageSize: pageSize.value })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return
  creating.value = true
  try {
    await createUser(createForm.value)
    ElMessage.success('创建成功')
    showCreate.value = false
    createForm.value = { username: '', password: '', realName: '', email: '', role: 'USER' }
    fetchList()
  } finally {
    creating.value = false
  }
}

async function handleStatusChange(row) {
  const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  try {
    await ElMessageBox.confirm(
      `确定${newStatus === 'ACTIVE' ? '启用' : '停用'}用户「${row.username}」？`,
      '提示'
    )
    await updateUserStatus(row.id, newStatus)
    ElMessage.success('状态已更新')
    fetchList()
  } catch {
    // 取消操作不处理
  }
}

function openPasswordDialog(row) {
  resetUserId.value = row.id
  newPassword.value = ''
  showPassword.value = true
}

async function handleResetPassword() {
  if (!newPassword.value) {
    ElMessage.warning('请输入新密码')
    return
  }
  resetting.value = true
  try {
    await resetUserPassword(resetUserId.value, newPassword.value)
    ElMessage.success('密码已重置')
    showPassword.value = false
  } finally {
    resetting.value = false
  }
}

onMounted(fetchList)
</script>
