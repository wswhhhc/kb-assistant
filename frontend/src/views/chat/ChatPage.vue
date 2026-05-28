<template>
  <div class="chat-container">
    <ChatSessionSidebar
      :kb-list="kbList"
      :sessions="sessions"
      v-model:current-kb-id="currentKbId"
      :current-session-id="currentSessionId"
      @create="createNewSession"
      @kb-change="handleKbChange"
      @switch-session="switchSession"
      @delete-session="handleDeleteSession"
    />

    <div class="chat-main">
      <template v-if="currentKbId">
        <ChatMessageList
          ref="msgListRef"
          :messages="messages"
          :loading="loading"
          @toggle-citation="toggleCitation"
          @open-citation="openCitation"
          @submit-feedback="(msg, type, reason, reasonType) => submitMessageFeedback(msg, type, reason, reasonType)"
          @open-dislike="openDislikeDialog"
        />

        <div class="input-area">
          <el-input
            v-model="question"
            type="textarea"
            :rows="3"
            placeholder="请输入你的问题..."
            :disabled="loading"
            @keydown.enter.prevent="handleSend"
          />
          <div class="input-footer">
            <el-button type="primary" @click="handleSend" :loading="loading" :disabled="!question.trim()">
              发送
            </el-button>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="empty-state">
          <el-icon :size="48" class="empty-icon"><ChatDotRound /></el-icon>
          <p class="empty-hint">请先选择一个知识库</p>
        </div>
      </template>
    </div>

    <CitationDialog
      :visible="citationDialogVisible"
      :citation="currentCitation"
      @update:visible="citationDialogVisible = $event"
      @go-source="goToDocumentSource"
    />

    <FeedbackDialog
      :visible="feedbackDialogVisible"
      :reason-type="feedbackReasonType"
      :reason="feedbackReason"
      :submitting="feedbackSubmitting"
      @update:visible="feedbackDialogVisible = $event"
      @update:reason-type="feedbackReasonType = $event"
      @update:reason="feedbackReason = $event"
      @confirm="confirmDislikeFeedback"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound } from '@element-plus/icons-vue'
import { getSession } from '@/api/chat'
import { listKnowledgeBases } from '@/api/knowledgeBase'
import ChatSessionSidebar from '@/components/chat/ChatSessionSidebar.vue'
import ChatMessageList from '@/components/chat/ChatMessageList.vue'
import CitationDialog from '@/components/chat/CitationDialog.vue'
import FeedbackDialog from '@/components/chat/FeedbackDialog.vue'
import { useCitationNavigation } from '@/composables/useCitationNavigation'
import { useChatSessions } from '@/composables/useChatSessions'
import { useChatStream } from '@/composables/useChatStream'

const route = useRoute()

const msgListRef = ref(null)
const scrollToBottom = () => msgListRef.value?.scrollToBottom()

const {
  citationDialogVisible,
  currentCitation,
  openCitation,
  goToDocumentSource,
} = useCitationNavigation()

const {
  kbList, sessions, currentKbId, currentSessionId, messages,
  fetchKbList, fetchSessions, switchSession,
  createNewSession, handleDeleteSession, handleKbChange,
  feedbackDialogVisible, feedbackReasonType, feedbackReason,
  feedbackSubmitting, submitMessageFeedback,
  openDislikeDialog, confirmDislikeFeedback,
} = useChatSessions({ onSessionSwitch: scrollToBottom })

const {
  question,
  loading,
  handleSend,
} = useChatStream({
  messages,
  currentSessionId,
  currentKbId,
  sessions,
  createNewSession,
  switchSession,
  fetchSessions,
  scrollToBottom,
})

function toggleCitation(idx) {
  messages.value[idx]._showCitations = !messages.value[idx]._showCitations
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
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 120px);
  gap: 16px;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.input-area {
  padding: 16px 20px;
  border-top: 1px solid #ebeef5;
}
.input-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.empty-state {
  text-align: center;
  color: #909399;
  margin-top: 200px;
}
.empty-icon {
  color: #dcdfe6;
}
.empty-hint {
  margin-top: 12px;
  font-size: 14px;
}
</style>
