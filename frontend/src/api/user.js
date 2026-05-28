import request from '@/utils/request'

export function listUsers(params) {
  return request.get('/users', { params })
}

export function createUser(data) {
  return request.post('/users', data)
}

export function updateUserStatus(id, status) {
  return request.put(`/users/${id}/status`, null, { params: { status } })
}

export function resetUserPassword(id, password) {
  return request.put(`/users/${id}/password`, null, { params: { password } })
}
