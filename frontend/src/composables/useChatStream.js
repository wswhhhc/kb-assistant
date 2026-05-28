import { ref, nextTick } from 'vue'
import { buildSessionTitle } from '@/utils/markdown'
import { streamRequest } from '@/utils/streamRequest'

export function useChatStream({
  messages,
  currentSessionId,
  currentKbId,
  sessions,
  createNewSession,
  switchSession,
  fetchSessions,
  scrollToBottom,
}) {
  const question = ref('')
  const loading = ref(false)

  async function handleSend() {
    const q = question.value.trim()
    if (!q || !currentKbId.value) return

    let sessionId = currentSessionId.value
    if (!sessionId) {
      const session = await createNewSession()
      if (!session) return
      sessionId = session.id
    }

    // 推入用户消息
    messages.value.push({ role: 'USER', content: q, citations: [] })
    question.value = ''

    // 更新会话标题
    const currentSession = sessions.value.find(s => s.id === sessionId)
    if (currentSession && currentSession.title === '新会话') {
      currentSession.title = buildSessionTitle(q)
    }

    loading.value = true

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
      let citations = []
      let fullAnswer = ''
      let streamFinished = false

      await streamRequest('/api/chat/ask-stream', {
        knowledgeBaseId: currentKbId.value,
        sessionId,
        question: q
      }, {
        onCitations(data) {
          citations = Array.isArray(data) ? data : []
          aiMsg.citations = citations
        },
        onAnswer(text) {
          if (!fullAnswer) {
            aiMsg._showThinking = false
          }
          fullAnswer += text
          aiMsg.content = fullAnswer
          scrollToBottom()
        },
        onDone(payload) {
          aiMsg.content = fullAnswer || payload.answer || ''
          citations = Array.isArray(payload.citations) ? payload.citations : citations
          aiMsg.citations = citations
          streamFinished = true
        },
        onError(err) {
          aiMsg.content = '请求失败，请稍后重试。'
          streamFinished = true
        }
      })

      // 流结束后刷新会话
      const savedThinking = aiMsg.thinkingContent
      await switchSession(sessionId)

      // 恢复思考过程（后端不存 thinkingContent）
      if (savedThinking) {
        const lastAiMsg = [...messages.value].reverse().find(m => m.role === 'AI')
        if (lastAiMsg) {
          lastAiMsg.thinkingContent = savedThinking
          lastAiMsg._showThinking = false
        }
      }
      await fetchSessions()

    } catch (err) {
      if (!aiMsg.content) {
        aiMsg.content = '请求失败，请稍后重试。'
      }
    } finally {
      loading.value = false
      scrollToBottom()
    }
  }

  return {
    question,
    loading,
    handleSend,
  }
}
