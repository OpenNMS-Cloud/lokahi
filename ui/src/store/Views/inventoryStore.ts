import { defineStore } from 'pinia'

export const useInventoryStore = defineStore('inventoryStore', {
  state: () => ({
    isTagsOpen: false
  }),
  actions: {
    triggerTagsBox() {
      this.isTagsOpen = !this.isTagsOpen
    }
  }
})
