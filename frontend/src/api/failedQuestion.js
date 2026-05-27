import request from '@/utils/request'

export function listFailedQuestions(params) {
  return request.get('/failed-questions', { params })
}
