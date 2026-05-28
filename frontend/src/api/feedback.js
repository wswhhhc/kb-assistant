import request from '@/utils/request'

export function submitFeedback(data) {
  return request.post('/feedback', data)
}

export function listFeedback(params) {
  return request.get('/feedback', { params })
}

export function getMyFeedbackMap(messageIds) {
  return request.get('/feedback/my', {
    params: { messageIds: messageIds.join(',') }
  })
}
