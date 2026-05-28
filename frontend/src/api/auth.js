import request from '@/utils/request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function getUserInfo() {
  return request.get('/auth/me')
}

export function logout() {
  return request.post('/auth/logout')
}
