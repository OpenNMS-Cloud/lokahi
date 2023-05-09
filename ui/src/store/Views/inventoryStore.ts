import { defineStore } from 'pinia'
import { InventoryNode } from '@/types/inventory'

export const useInventoryStore = defineStore('inventoryStore', {
  state: () => ({
    isTagManagerOpen: false,
    isTagManagerReset: false,
    isFilterOpen: false,
    nodesSelected: [] as InventoryNode[],
    isEditMode: false
  }),
  actions: {
    toggleTagManager() {
      this.isTagManagerOpen = !this.isTagManagerOpen
    },
    toggleFilter() {
      this.isFilterOpen = !this.isFilterOpen
    },
    toggleNodeEditMode() {
      this.isEditMode = !this.isEditMode
    },
    resetNodeEditMode() {
      this.isEditMode = false
    },
    addRemoveNodesSelected(node: InventoryNode, isSelected: boolean) {
      if (isSelected) {
        const isNodeAlreadySelected = this.nodesSelected.some(({ id }) => id === node.id)
        if (!isNodeAlreadySelected) this.nodesSelected.push(node)
      } else {
        this.nodesSelected = this.nodesSelected.filter(({ id }) => id !== node.id)
      }
    },
    resetSelectedNode() {
      this.nodesSelected = []
    }
  }
})
