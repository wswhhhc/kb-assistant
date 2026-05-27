import request from '@/utils/request'

export function askQuestion(data) {
  return request.post('/chat/ask', data)
}

export function listSessions(params) {
  return request.get('/chat/sessions', { params })
}

export function getSessionMessages(sessionId) {
  return request.get(`/chat/sessions/${sessionId}/messages`)
}

export function createSession(data) {
  return request.post('/chat/sessions', data)
}

export function deleteSession(id) {
  return request.delete(`/chat/sessions/${id}`)
}
