import { defineStore } from 'pinia'

export const useChatStore = defineStore('chat', {
  state: () => ({
    sessions: [],
    currentSessionId: null,
    messages: []
  }),
  actions: {
    setCurrentSession(sessionId) {
      this.currentSessionId = sessionId
    },
    setSessions(sessions) {
      this.sessions = sessions
    },
    setMessages(messages) {
      this.messages = messages
    },
    addMessage(msg) {
      this.messages.push(msg)
    },
    clearMessages() {
      this.messages = []
    }
  }
})
