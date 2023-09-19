import { defineStore } from 'pinia'

export const useAppStore = defineStore('appStore', {
  getters: {
    isCloud() {
      if (location.origin.includes('opennms.com')) {
        return true
      }
      return false
    }
  }
})
