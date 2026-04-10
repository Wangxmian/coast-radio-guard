import { defineStore } from 'pinia'
import { login as loginApi } from '../api/auth'

const TOKEN_KEY = 'crg_token'
const USER_INFO_KEY = 'crg_user_info'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: JSON.parse(localStorage.getItem(USER_INFO_KEY) || 'null')
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    async login(payload) {
      const data = await loginApi(payload)
      this.token = data.token
      this.userInfo = {
        username: data.username,
        role: data.role,
        tokenType: data.tokenType,
        expireSeconds: data.expireSeconds
      }
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(this.userInfo))
      return data
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_INFO_KEY)
    }
  }
})
