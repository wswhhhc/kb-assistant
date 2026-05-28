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

export function getSession(sessionId) {
  return request.get(`/chat/sessions/${sessionId}`)
}

export function createSession(params) {
  return request.post('/chat/sessions', null, { params })
}

export function deleteSession(id) {
  return request.delete(`/chat/sessions/${id}`)
}

export { streamRequest } from '@/utils/streamRequest'
