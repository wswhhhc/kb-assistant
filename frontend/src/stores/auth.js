import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null
  }),
  getters: {
    isLoggedIn: state => !!state.token,
    isAdmin: state => state.userInfo?.role === 'ADMIN'
  },
  actions: {
    async login(username, password) {
      const res = await request.post('/auth/login', { username, password })
      this.token = res.data.token
      localStorage.setItem('token', res.data.token)
      this.userInfo = res.data
    },
    async getUserInfo() {
      const res = await request.get('/auth/me')
      this.userInfo = res.data
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
    }
  }
})
