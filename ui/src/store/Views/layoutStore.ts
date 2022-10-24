import { defineStore } from 'pinia'

export const useLayoutStore = defineStore('layoutStore', {
  state: () => ({
    widgetBarOpen: false
  }),
  actions: {
    triggerWidgetBar() {
      this.widgetBarOpen = !this.widgetBarOpen
    }
  }
})
