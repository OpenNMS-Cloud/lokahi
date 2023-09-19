import { defineStore } from 'pinia'

export const useAppStore = defineStore('appStore', {
  getters: {
    isCloud() {
      return Boolean(location.origin.includes('opennms.com'))
    }
  }
})
