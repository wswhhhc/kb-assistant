<template>
  <div class="chat-container">
    <div class="chat-sidebar">
      <div style="padding: 12px;">
        <el-button type="primary" style="width: 100%;" @click="createNewSession">新建会话</el-button>
      </div>
      <div style="padding: 0 12px 12px;">
        <el-select v-model="currentKbId" placeholder="选择知识库" style="width: 100%;" @change="handleKbChange">
          <el-option v-for="kb in kbList" :key="kb.id" :label="kb.name" :value="kb.id" />
        </el-select>
      </div>
      <el-scrollbar style="flex: 1;">
        <div v-for="s in sessions" :key="s.id"
          class="session-item"
          :class="{ active: s.id === currentSessionId }"
          @click="switchSession(s.id)"
        >
          <div class="session-title">{{ s.title }}</div>
          <div class="session-time">{{ formatTime(s.updatedAt) }}</div>
          <el-button text size="small" type="danger" class="session-del"
            @click.stop="handleDeleteSession(s.id)">×</el-button>
        </div>
        <div v-if="sessions.length === 0" style="text-align: center; color: #909399; padding: 20px; font-size: 13px;">
          暂无会话
        </div>
      </el-scrollbar>
    </div>

    <div class="chat-main">
      <template v-if="currentKbId">
        <el-scrollbar ref="msgScroll" class="message-area">
          <div v-for="(msg, idx) in messages" :key="idx" class="message-wrapper">
            <div v-if="msg.role === 'USER'" class="message-row user-row">
              <div class="message-bubble user-bubble">{{ msg.content }}</div>
            </div>
            <div v-else class="message-row ai-row">
              <div class="message-bubble ai-bubble">
                <div v-if="msg.thinkingContent" class="thinking-section">
                  <div class="thinking-header" @click="msg._showThinking = !msg._showThinking">
                    <span class="thinking-icon">✦</span>
                    <span>{{ msg._showThinking ? '收起思考过程' : '展开思考过程' }}</span>
                    <span class="thinking-toggle">{{ msg._showThinking ? '▲' : '▼' }}</span>
                  </div>
                  <div v-show="msg._showThinking" class="thinking-body">
                    {{ msg.thinkingContent }}
                  </div>
                </div>
                <div class="ai-content" v-html="renderMarkdown(msg.content, msg.citations, idx)"></div>
                <div v-if="msg.id" class="feedback-actions">
                  <el-button
                    size="small"
                    text
                    :type="msg.feedbackType === 'LIKE' ? 'success' : 'default'"
                    :loading="msg._feedbackLoading"
                    @click="submitMessageFeedback(msg, 'LIKE')"
                  >
                    <el-icon><CircleCheck /></el-icon>
                    <span style="margin-left: 4px;">有帮助</span>
                  </el-button>
                  <el-button
                    size="small"
                    text
                    :type="msg.feedbackType === 'DISLIKE' ? 'danger' : 'default'"
                    :loading="msg._feedbackLoading"
                    @click="openDislikeDialog(msg)"
                  >
                    <el-icon><CircleClose /></el-icon>
                    <span style="margin-left: 4px;">需要改进</span>
                  </el-button>
                </div>
                <div v-if="msg.citations && msg.citations.length > 0" class="citation-section">
                  <el-divider style="margin: 8px 0;" />
                  <div class="citation-header" @click="toggleCitation(idx)">
                    <span style="font-size: 12px; color: #909399;">引用来源（{{ msg.citations.length }} 条）</span>
                    <el-icon :class="{ rotated: msg._showCitations }" style="transition: transform 0.2s;">
                      <ArrowDown />
                    </el-icon>
                  </div>
                  <template v-if="msg._showCitations">
                    <div v-for="(c, ci) in msg.citations" :key="ci"
                      class="citation-item"
                      :id="'citation-ref-' + idx + '-' + ci"
                      :class="{ 'citation-highlighted': msg._highlightedIdx === ci }"
                      @click="openCitation(c)">
                      <el-tag size="small" type="info" style="margin-right: 4px; cursor: pointer;">{{ ci + 1 }}</el-tag>
                      <span style="font-size: 12px; cursor: pointer;">{{ c.fileName }} — 片段 {{ c.chunkIndex }}</span>
                      <span v-if="c.pageNo" style="font-size: 12px; color: #909399; margin-left: auto;">第 {{ c.pageNo }} 页</span>
                    </div>
                  </template>
                </div>
              </div>
            </div>
          </div>
          <div v-if="messages.length === 0 && !loading" style="text-align: center; color: #909399; margin-top: 120px;">
            <el-icon :size="48" style="color: #dcdfe6;"><ChatDotRound /></el-icon>
            <p style="margin-top: 12px; font-size: 14px;">输入问题开始问答</p>
          </div>
        </el-scrollbar>

        <div class="input-area">
          <el-input
            v-model="question"
            type="textarea"
            :rows="3"
            placeholder="请输入你的问题..."
            :disabled="loading"
            @keydown.enter.prevent="handleSend"
          />
          <div style="display: flex; justify-content: flex-end; margin-top: 8px;">
            <el-button type="primary" @click="handleSend" :loading="loading" :disabled="!question.trim()">
              发送
            </el-button>
          </div>
        </div>
      </template>

      <template v-else>
        <div style="text-align: center; color: #909399; margin-top: 200px;">
          <el-icon :size="48" style="color: #dcdfe6;"><ChatDotRound /></el-icon>
          <p style="margin-top: 12px; font-size: 14px;">请先选择一个知识库</p>
        </div>
      </template>
    </div>

    <el-dialog v-model="citationDialogVisible" title="引用内容" width="600px">
      <div v-if="currentCitation" style="font-size: 14px; line-height: 1.8;">
        <div style="margin-bottom: 12px; color: #909399; font-size: 12px;">
          来源：{{ currentCitation.fileName }} — 片段 {{ currentCitation.chunkIndex }}
          <span v-if="currentCitation.pageNo"> — 第 {{ currentCitation.pageNo }} 页</span>
        </div>
        <div style="background: #f5f7fa; padding: 16px; border-radius: 8px; white-space: pre-wrap; word-break: break-word;">
          {{ currentCitation.content }}
        </div>
        <div style="display: flex; justify-content: flex-end; margin-top: 12px;">
          <el-button type="primary" link @click="goToDocumentSource(currentCitation)" :disabled="!currentCitation.documentId">
            查看文档原文定位
          </el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="feedbackDialogVisible" title="反馈原因" width="420px">
      <el-form label-width="0">
        <el-form-item>
          <el-radio-group v-model="feedbackReasonType">
            <el-radio-button label="LOW_QUALITY">答非所问</el-radio-button>
            <el-radio-button label="NO_HIT">没检索到重点</el-radio-button>
            <el-radio-button label="INSUFFICIENT_CITATION">引用不充分</el-radio-button>
            <el-radio-button label="MODEL_ERROR">内容有误</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="feedbackReason"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入这条回答哪里需要改进（选填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="feedbackSubmitting" @click="confirmDislikeFeedback">提交反馈</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { getSession, listSessions, getSessionMessages, createSession, deleteSession } from '@/api/chat'
import { submitFeedback, getMyFeedbackMap } from '@/api/feedback'
import { listKnowledgeBases } from '@/api/knowledgeBase'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, ArrowDown, CircleCheck, CircleClose } from '@element-plus/icons-vue'

const chatStore = useChatStore()
const router = useRouter()
const route = useRoute()
const msgScroll = ref(null)

const currentKbId = ref(null)
const kbList = ref([])
const sessions = ref([])
const messages = ref([])
const question = ref('')
const loading = ref(false)
const currentSessionId = ref(null)

const citationDialogVisible = ref(false)
const currentCitation = ref(null)
const feedbackDialogVisible = ref(false)
const feedbackReasonType = ref('LOW_QUALITY')
const feedbackReason = ref('')
const feedbackSubmitting = ref(false)
const pendingFeedbackMessage = ref(null)

function normalizeMessage(message) {
  let citations = []
  try {
    if (message.citationJson) citations = JSON.parse(message.citationJson)
  } catch {}

  return {
    ...message,
    citations,
    feedbackType: message.feedbackType || null,
    _showCitations: false,
    _feedbackLoading: false,
    _highlightedIdx: null
  }
}

function toggleCitation(idx) {
  messages.value[idx]._showCitations = !messages.value[idx]._showCitations
}

function showCitationContent(c) {
  currentCitation.value = c
  citationDialogVisible.value = true
}

function openCitation(c) {
  showCitationContent(c)
}

function formatTime(t) {
  if (!t) return ''
  return t.substring(0, 16).replace('T', ' ')
}

function buildSessionTitle(text) {
  const normalized = (text || '').replace(/\s+/g, ' ').trim()
  if (!normalized) return '新会话'
  return normalized.length > 20 ? `${normalized.slice(0, 20)}...` : normalized
}

function renderMarkdown(text, citations = null, msgIdx = -1) {
  if (!text) return ''
  const escaped = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.+?)`/g, '<code style="background:#f4f4f5;padding:1px 4px;border-radius:3px;font-size:0.9em;">$1</code>')

  if (citations && citations.length > 0 && msgIdx >= 0) {
    return escaped
      .replace(/\n/g, '<br>')
      .replace(/\[(\d+)\]/g, (match, num) => {
        const ci = parseInt(num) - 1
        if (ci >= 0 && ci < citations.length) {
          return `<a class="citation-marker" href="javascript:void(0)" onclick="window.__goToCitation(${msgIdx}, ${ci})" title="${citations[ci].fileName}">[${num}]</a>`
        }
        return match
      })
  }

  return escaped.replace(/\n/g, '<br>')
}

async function fetchKbList() {
  try {
    const res = await listKnowledgeBases({ pageNum: 1, pageSize: 100 })
    kbList.value = res.data.records || []
    if (kbList.value.length > 0) {
      currentKbId.value = kbList.value[0].id
      fetchSessions()
    }
  } catch {}
}

async function fetchSessions() {
  if (!currentKbId.value) return
  try {
    const res = await listSessions({
      pageNum: 1,
      pageSize: 50,
      knowledgeBaseId: currentKbId.value
    })
    sessions.value = res.data.records || []
  } catch {}
}

async function switchSession(sessionId) {
  currentSessionId.value = sessionId
  chatStore.setCurrentSession(sessionId)
  try {
    const res = await getSessionMessages(sessionId)
    messages.value = (res.data.records || []).map(normalizeMessage)
    await loadFeedbackStatus()
    await nextTick()
    scrollToBottom()
  } catch {}
}

async function createNewSession() {
  if (!currentKbId.value) {
    ElMessage.warning('请先选择知识库')
    return
  }
  try {
    const res = await createSession({ knowledgeBaseId: currentKbId.value })
    const session = res.data
    sessions.value.unshift(session)
    currentSessionId.value = session.id
    messages.value = []
    chatStore.setCurrentSession(session.id)
  } catch {}
}

async function handleDeleteSession(id) {
  try {
    await ElMessageBox.confirm('确定删除该会话？', '提示')
    await deleteSession(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    if (currentSessionId.value === id) {
      currentSessionId.value = null
      messages.value = []
    }
  } catch {}
}

function handleKbChange() {
  currentSessionId.value = null
  messages.value = []
  fetchSessions()
}

function getToken() {
  return localStorage.getItem('token') || ''
}

async function loadFeedbackStatus() {
  const aiMessageIds = messages.value
    .filter(msg => msg.role === 'AI' && msg.id)
    .map(msg => msg.id)

  if (aiMessageIds.length === 0) return

  try {
    const res = await getMyFeedbackMap(aiMessageIds)
    const feedbackMap = res.data || {}
    messages.value.forEach(msg => {
      if (msg.role === 'AI' && msg.id) {
        msg.feedbackType = feedbackMap[msg.id] || null
      }
    })
  } catch {}
}

async function submitMessageFeedback(message, feedbackType, reason = '', reasonType = 'LOW_QUALITY') {
  if (!message?.id) {
    ElMessage.warning('当前消息还未保存完成，请稍后再试')
    return
  }

  message._feedbackLoading = true
  try {
    await submitFeedback({
      messageId: message.id,
      feedbackType,
      reasonType,
      reason
    })
    message.feedbackType = feedbackType
    ElMessage.success(feedbackType === 'LIKE' ? '已记录点赞' : '已记录反馈')
  } finally {
    message._feedbackLoading = false
  }
}

function openDislikeDialog(message) {
  pendingFeedbackMessage.value = message
  feedbackReasonType.value = 'LOW_QUALITY'
  feedbackReason.value = ''
  feedbackDialogVisible.value = true
}

async function confirmDislikeFeedback() {
  if (!pendingFeedbackMessage.value) return
  feedbackSubmitting.value = true
  try {
    await submitMessageFeedback(
      pendingFeedbackMessage.value,
      'DISLIKE',
      feedbackReason.value.trim(),
      feedbackReasonType.value
    )
    feedbackDialogVisible.value = false
    pendingFeedbackMessage.value = null
    feedbackReasonType.value = 'LOW_QUALITY'
    feedbackReason.value = ''
  } finally {
    feedbackSubmitting.value = false
  }
}

async function handleSend() {
  const q = question.value.trim()
  if (!q || !currentKbId.value) return

  if (!currentSessionId.value) {
    try {
      const res = await createSession({ knowledgeBaseId: currentKbId.value })
      currentSessionId.value = res.data.id
      sessions.value.unshift(res.data)
    } catch {
      return
    }
  }

  messages.value.push({ role: 'USER', content: q, citations: [] })
  question.value = ''
  loading.value = true

  const currentSession = sessions.value.find(s => s.id === currentSessionId.value)
  if (currentSession && currentSession.title === '新会话') {
    currentSession.title = buildSessionTitle(q)
  }

  // 创建 AI 消息占位
  messages.value.push({
    role: 'AI',
    content: '',
    thinkingContent: '',
    _showThinking: true,
    citations: [],
    feedbackType: null,
    _showCitations: false,
    _feedbackLoading: false
  })
  const aiMsg = messages.value[messages.value.length - 1]
  scrollToBottom()

  try {
    const token = getToken()
    const body = JSON.stringify({
      knowledgeBaseId: currentKbId.value,
      sessionId: currentSessionId.value,
      question: q
    })
    const response = await fetch('/api/chat/ask-stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: token ? `Bearer ${token}` : ''
      },
      body
    })

    if (!response.ok) {
      if (response.status === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
        return
      }
      aiMsg.content = '请求失败，请稍后重试。'
      return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let currentEvent = ''
    let dataLines = []
    let citations = []
    let fullAnswer = ''
    let streamFinished = false

    const processEvent = () => {
      if (!currentEvent || dataLines.length === 0) return false
      const dataText = dataLines.join('\n')

      try {
        const data = JSON.parse(dataText)
        if (currentEvent === 'citations') {
          citations = Array.isArray(data) ? data : []
          aiMsg.citations = citations
        } else if (currentEvent === 'thinking') {
          if (data.text !== undefined) {
            aiMsg.thinkingContent += data.text
          }
        } else if (currentEvent === 'answer') {
          if (data.text !== undefined) {
            // 首个 answer 到达时自动折叠思考区域
            if (!fullAnswer) {
              aiMsg._showThinking = false
            }
            fullAnswer += data.text
            aiMsg.content = fullAnswer
          }
        } else if (currentEvent === 'done') {
          if (!fullAnswer && data.answer) {
            fullAnswer = data.answer
          }
          aiMsg.content = fullAnswer || data.answer || ''
          citations = Array.isArray(data.citations) ? data.citations : citations
          aiMsg.citations = citations
          streamFinished = true
        }
      } catch {}

      currentEvent = ''
      dataLines = []
      return streamFinished
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split(/\r?\n/)
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (!line) {
          const finished = processEvent()
          nextTick().then(scrollToBottom)
          if (finished) {
            await reader.cancel()
            break
          }
          continue
        }

        if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim()
          continue
        }

        if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).trim())
        }
      }

      if (streamFinished) break
    }

    if (!streamFinished && buffer.trim()) {
      const trailingLines = buffer.split(/\r?\n/)
      for (const line of trailingLines) {
        if (!line) {
          processEvent()
        } else if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).trim())
        }
      }
    }
    if (!streamFinished) processEvent()

    aiMsg.content = fullAnswer
    aiMsg.citations = citations
    await switchSession(currentSessionId.value)
    await fetchSessions()

  } catch {
    if (!aiMsg.content) {
      aiMsg.content = '请求失败，请稍后重试。'
    }
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function scrollToBottom() {
  if (msgScroll.value) {
    const wrap = msgScroll.value.wrapRef
    if (wrap) wrap.scrollTop = wrap.scrollHeight
  }
}

function goToCitation(msgIdx, ci) {
  const msg = messages.value[msgIdx]
  if (!msg) return

  msg._showCitations = true
  msg._highlightedIdx = ci

  nextTick(() => {
    const el = document.getElementById('citation-ref-' + msgIdx + '-' + ci)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
      el.classList.add('citation-flash')
      setTimeout(() => el.classList.remove('citation-flash'), 1500)
    }
  })
}

function goToDocumentSource(citation) {
  citationDialogVisible.value = false
  if (!citation?.documentId || !currentKbId.value) return

  router.push({
    name: 'DocumentDetail',
    params: {
      kbId: currentKbId.value,
      documentId: citation.documentId
    },
    query: {
      chunkIndex: citation.chunkIndex
    }
  })
}

onMounted(async () => {
  const targetSessionId = route.query.sessionId
  if (targetSessionId) {
    try {
      const sessionRes = await getSession(targetSessionId)
      const session = sessionRes.data
      const res = await listKnowledgeBases({ pageNum: 1, pageSize: 100 })
      kbList.value = res.data.records || []
      currentKbId.value = session.knowledgeBaseId
      await fetchSessions()
      await switchSession(Number(targetSessionId))
    } catch {
      fetchKbList()
    }
  } else {
    fetchKbList()
  }
  window.__goToCitation = goToCitation
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 120px);
  gap: 16px;
}
.chat-sidebar {
  width: 260px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
.session-item {
  position: relative;
  padding: 10px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}
.session-item:hover { background: #f5f7fa; }
.session-item.active { background: #ecf5ff; }
.session-title {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 20px;
}
.session-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 2px;
}
.session-del {
  position: absolute;
  right: 4px;
  top: 4px;
  display: none;
}
.session-item:hover .session-del { display: block; }

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
.message-area {
  flex: 1;
  padding: 20px;
}
.message-wrapper {
  margin-bottom: 16px;
}
.message-row {
  display: flex;
}
.user-row { justify-content: flex-end; }
.ai-row { justify-content: flex-start; }
.message-bubble {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
}
.user-bubble {
  background: #409eff;
  color: #fff;
}
.ai-bubble {
  background: #f5f7fa;
  color: #303133;
}
.ai-content { word-break: break-word; }
.feedback-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.citation-section { margin-top: 4px; }
.citation-header {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 4px 0;
  user-select: none;
}
.citation-header:hover { color: #409eff; }
.rotated { transform: rotate(180deg); }
:deep(.citation-marker) {
  color: #409eff;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  padding: 0 1px;
}
:deep(.citation-marker:hover) {
  color: #1a6bb5;
  background: #ecf5ff;
  border-radius: 2px;
}
.citation-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 6px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
}
.citation-item:hover { background: #e8edf3; }
.citation-highlighted { background: #ecf5ff !important; border-left: 3px solid #409eff; }
.citation-flash {
  animation: citationFlash 1.5s ease-out;
}
@keyframes citationFlash {
  0% { background: #ecf5ff; }
  50% { background: #b3d8ff; }
  100% { background: #ecf5ff; }
}
.thinking-section {
  background: #f0f2f5;
  border-radius: 8px;
  margin-bottom: 12px;
  border-left: 3px solid #409eff;
}
.thinking-header {
  padding: 6px 12px;
  cursor: pointer;
  user-select: none;
  font-size: 13px;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 6px;
}
.thinking-header:hover {
  color: #409eff;
}
.thinking-icon {
  font-size: 12px;
  color: #409eff;
}
.thinking-toggle {
  margin-left: auto;
  font-size: 10px;
}
.thinking-body {
  padding: 0 12px 10px;
  font-size: 13px;
  color: #909399;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-all;
}
.input-area {
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
}
</style>
