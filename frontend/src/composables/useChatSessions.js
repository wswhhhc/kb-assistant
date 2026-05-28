import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listSessions, getSessionMessages, createSession, deleteSession, getSession } from '@/api/chat'
import { listKnowledgeBases } from '@/api/knowledgeBase'
import { submitFeedback, getMyFeedbackMap } from '@/api/feedback'
import { buildSessionTitle } from '@/utils/markdown'

export function useChatSessions({ onSessionSwitch }) {
  const kbList = ref([])
  const sessions = ref([])
  const currentKbId = ref(null)
  const currentSessionId = ref(null)
  const messages = ref([])

  // 反馈弹窗状态
  const feedbackDialogVisible = ref(false)
  const feedbackReasonType = ref('LOW_QUALITY')
  const feedbackReason = ref('')
  const feedbackSubmitting = ref(false)
  const pendingFeedbackMessage = ref(null)

  async function fetchKbList() {
    try {
      const res = await listKnowledgeBases({ pageNum: 1, pageSize: 100 })
      kbList.value = res.data.records || []
      if (kbList.value.length > 0 && !currentKbId.value) {
        currentKbId.value = kbList.value[0].id
        await fetchSessions()
      }
    } catch {
      console.warn('获取知识库列表失败')
    }
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
    } catch {
      console.warn('获取会话列表失败')
    }
  }

  function normalizeMessage(message) {
    let citations = []
    try {
      if (message.citationJson) citations = JSON.parse(message.citationJson)
    } catch {
      console.warn('引用 JSON 解析失败', message.id)
    }
    return {
      ...message,
      citations,
      feedbackType: message.feedbackType || null,
      _showCitations: false,
      _feedbackLoading: false,
      _highlightedIdx: null
    }
  }

  async function switchSession(sessionId) {
    currentSessionId.value = sessionId
    try {
      const res = await getSessionMessages(sessionId)
      messages.value = (res.data.records || []).map(normalizeMessage)
      await loadFeedbackStatus()
      onSessionSwitch?.()
    } catch {
      console.warn('加载会话消息失败')
    }
  }

  async function createNewSession() {
    if (!currentKbId.value) {
      ElMessage.warning('请先选择知识库')
      return null
    }
    try {
      const res = await createSession({ knowledgeBaseId: currentKbId.value })
      const session = res.data
      sessions.value.unshift(session)
      currentSessionId.value = session.id
      messages.value = []
      return session
    } catch {
      console.warn('创建会话失败')
      return null
    }
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
    } catch {
      // 取消操作不处理
    }
  }

  function handleKbChange() {
    currentSessionId.value = null
    messages.value = []
    fetchSessions()
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
    } catch {
      console.warn('加载反馈状态失败')
    }
  }

  async function submitMessageFeedback(message, feedbackType, reason = '', reasonType = 'LOW_QUALITY') {
    if (!message?.id) {
      ElMessage.warning('当前消息还未保存完成，请稍后再试')
      return
    }
    message._feedbackLoading = true
    try {
      await submitFeedback({ messageId: message.id, feedbackType, reasonType, reason })
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

  return {
    kbList, sessions, currentKbId, currentSessionId, messages,
    fetchKbList, fetchSessions, switchSession,
    createNewSession, handleDeleteSession, handleKbChange,
    normalizeMessage,
    feedbackDialogVisible, feedbackReasonType, feedbackReason,
    feedbackSubmitting, pendingFeedbackMessage,
    loadFeedbackStatus, submitMessageFeedback,
    openDislikeDialog, confirmDislikeFeedback,
  }
}
