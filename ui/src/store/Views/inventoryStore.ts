import { defineStore } from 'pinia'
import { InventoryNode } from '@/types/inventory'
import { Tag } from '@/types/graphql'

export const useInventoryStore = defineStore('inventoryStore', {
  state: () => ({
    isTagManagerOpen: false,
    isTagManagerReset: false,
    isFilterOpen: false,
    nodesSelected: [] as InventoryNode[],
    searchType: { id: 1, name: 'Labels' },
    tagsSelected: [] as Tag[],
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
    addSelectedTag(beep: Tag[]) {
      this.tagsSelected = beep;
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
    setSearchType(searchType: { id: number, name: string }) {
      this.searchType = searchType;
    },
    resetSelectedNode() {
      this.nodesSelected = []
    }
  }
})
