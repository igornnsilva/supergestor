import { defineStore } from 'pinia'
import { authApi } from '../services/api'

const STORAGE_KEY = 'supergestor_auth'

function loadSavedAuth() {
  const savedAuth = localStorage.getItem(STORAGE_KEY)
  return savedAuth ? JSON.parse(savedAuth) : null
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    session: loadSavedAuth()
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.session?.token),
    user: (state) => state.session
  },
  actions: {
    async login(credentials) {
      const session = await authApi.login(credentials)
      this.session = session
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
      return session
    },
    logout() {
      this.session = null
      localStorage.removeItem(STORAGE_KEY)
    }
  }
})
