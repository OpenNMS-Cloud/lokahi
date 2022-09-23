import { defineStore } from 'pinia'

export const useLayoutStore = defineStore('layoutStore', {
  state: () => ({
    navRailOpen: false,
    widgetBarOpen: false
  }),
  actions: {
    triggerNavRail() {
      this.navRailOpen = !this.navRailOpen
    },
    triggerWidgetBar() {
      this.widgetBarOpen = !this.widgetBarOpen
    }
  }
})
