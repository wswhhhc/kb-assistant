import request from '@/utils/request'

export function listFailedQuestions(params) {
  return request.get('/failed-questions', { params })
}

export function getFailedQuestionStats() {
  return request.get('/failed-questions/stats')
}

export function updateFailedQuestionStatus(id, status, resolution) {
  return request.put(`/failed-questions/${id}/status`, null, {
    params: { status, resolution }
  })
}
