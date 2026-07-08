import { defineStore } from 'pinia'

export const useNotificationsStore = defineStore('notifications', {
  state: () => ({
    message: '',
    type: 'success'
  }),
  actions: {
    show(message, type = 'success') {
      this.message = message
      this.type = type
      window.setTimeout(() => {
        this.message = ''
      }, 3500)
    }
  }
})

