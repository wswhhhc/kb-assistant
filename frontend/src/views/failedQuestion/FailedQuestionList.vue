<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
      <h3 style="margin: 0;">失败问题分析</h3>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom: 20px;">
      <el-col :span="4" v-for="item in statCards" :key="item.label">
        <el-card shadow="never" :body-style="{ padding: '14px' }">
          <div style="text-align: center;">
            <div :style="{ fontSize: '24px', fontWeight: 'bold', color: item.color }">{{ item.value }}</div>
            <div style="margin-top: 4px; color: #909399; font-size: 13px;">{{ item.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 筛选 -->
    <el-card shadow="never" style="margin-bottom: 16px;">
      <el-form :inline="true" style="margin-bottom: 0;">
        <el-form-item label="知识库">
          <el-select v-model="filterKbId" placeholder="全部知识库" clearable style="width: 200px;" @change="fetchList">
            <el-option v-for="kb in kbList" :key="kb.id" :label="kb.name" :value="kb.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="失败类型">
          <el-select v-model="filterType" placeholder="全部类型" clearable style="width: 150px;" @change="fetchList">
            <el-option label="无召回" value="NO_HIT" />
            <el-option label="回答质量差" value="LOW_QUALITY" />
            <el-option label="模型异常" value="MODEL_ERROR" />
            <el-option label="引用不足" value="INSUFFICIENT_CITATION" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 150px;" @change="fetchList">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已查看" value="REVIEWED" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已忽略" value="DISMISSED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchList">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="question" label="问题内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="失败类型" width="130">
          <template #default="{ row }">
            <el-tag :type="tagTypeMap[row.failureType] || 'info'" size="small">
              {{ labelMap[row.failureType] || row.failureType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="plain">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="kbName" label="所属知识库" width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.kbName || '知识库 ' + row.knowledgeBaseId }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注/原因" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.remark || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="记录时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" type="primary" link size="small" @click="handleUpdateStatus(row.id, 'REVIEWED')">标记已查看</el-button>
            <el-button v-if="row.status === 'PENDING'" type="success" link size="small" @click="openResolveDialog(row.id)">已解决</el-button>
            <el-button v-if="row.status === 'PENDING'" type="warning" link size="small" @click="openDismissDialog(row.id)">忽略</el-button>
            <span v-if="row.status !== 'PENDING'" style="color: #909399; font-size: 12px;">
              {{ row.status === 'REVIEWED' ? '已查看' : row.status === 'RESOLVED' ? '已解决' : '已忽略' }}
            </span>
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

    <!-- 处理说明对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="400px">
      <el-form label-width="60px">
        <el-form-item label="说明">
          <el-input v-model="dialogResolution" type="textarea" :rows="3" placeholder="请输入处理说明（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDialog" :loading="updating">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listFailedQuestions, getFailedQuestionStats, updateFailedQuestionStatus } from '@/api/failedQuestion'
import { listKnowledgeBases } from '@/api/knowledgeBase'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const updating = ref(false)
const tableData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const filterKbId = ref(null)
const filterType = ref(null)
const filterStatus = ref(null)
const kbList = ref([])
const stats = ref(null)

const labelMap = {
  NO_HIT: '无召回',
  LOW_QUALITY: '回答质量差',
  MODEL_ERROR: '模型异常',
  INSUFFICIENT_CITATION: '引用不足'
}

const tagTypeMap = {
  NO_HIT: 'warning',
  LOW_QUALITY: 'danger',
  MODEL_ERROR: 'info',
  INSUFFICIENT_CITATION: ''
}

function statusLabel(status) {
  const map = { PENDING: '待处理', REVIEWED: '已查看', RESOLVED: '已解决', DISMISSED: '已忽略' }
  return map[status] || status
}

function statusTagType(status) {
  const map = { PENDING: 'danger', REVIEWED: 'warning', RESOLVED: 'success', DISMISSED: 'info' }
  return map[status] || 'info'
}

const statCards = ref([
  { label: '总失败问题', value: '-', color: '#409eff' },
  { label: '无召回', value: '-', color: '#e6a23c' },
  { label: '回答质量差', value: '-', color: '#f56c6c' },
  { label: '模型异常', value: '-', color: '#909399' },
  { label: '引用不足', value: '-', color: '#67c23a' },
  { label: '待处理', value: '-', color: '#f56c6c' }
])

// 处理说明对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogTargetId = ref(null)
const dialogTargetStatus = ref('')
const dialogResolution = ref('')

function openResolveDialog(id) {
  dialogTargetId.value = id
  dialogTargetStatus.value = 'RESOLVED'
  dialogTitle.value = '标记为已解决'
  dialogResolution.value = ''
  dialogVisible.value = true
}

function openDismissDialog(id) {
  dialogTargetId.value = id
  dialogTargetStatus.value = 'DISMISSED'
  dialogTitle.value = '标记为已忽略'
  dialogResolution.value = ''
  dialogVisible.value = true
}

async function confirmDialog() {
  updating.value = true
  try {
    await updateFailedQuestionStatus(dialogTargetId.value, dialogTargetStatus.value, dialogResolution.value || null)
    ElMessage.success('状态更新成功')
    dialogVisible.value = false
    fetchList()
    fetchStats()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    updating.value = false
  }
}

async function handleUpdateStatus(id, status) {
  updating.value = true
  try {
    await updateFailedQuestionStatus(id, status, null)
    ElMessage.success('状态更新成功')
    fetchList()
    fetchStats()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    updating.value = false
  }
}

function formatTime(t) {
  if (!t) return ''
  return t.substring(0, 19).replace('T', ' ')
}

async function fetchStats() {
  try {
    const res = await getFailedQuestionStats()
    const data = res.data
    if (data) {
      statCards.value = [
        { label: '总失败问题', value: data.totalCount ?? 0, color: '#409eff' },
        { label: '无召回', value: data.noHitCount ?? 0, color: '#e6a23c' },
        { label: '回答质量差', value: data.lowQualityCount ?? 0, color: '#f56c6c' },
        { label: '模型异常', value: data.modelErrorCount ?? 0, color: '#909399' },
        { label: '引用不足', value: data.insufficientCitationCount ?? 0, color: '#67c23a' },
        { label: '待处理', value: data.pendingCount ?? 0, color: '#f56c6c' }
      ]
    }
  } catch {}
}

async function fetchKbList() {
  try {
    const res = await listKnowledgeBases({ pageNum: 1, pageSize: 100 })
    kbList.value = res.data.records || []
  } catch {}
}

async function fetchList() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filterKbId.value) params.kbId = filterKbId.value
    if (filterType.value) params.failureType = filterType.value
    if (filterStatus.value) params.status = filterStatus.value
    const res = await listFailedQuestions(params)
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchKbList()
  fetchStats()
  fetchList()
})
</script>
