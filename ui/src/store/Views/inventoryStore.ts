import { defineStore } from 'pinia'
import { InventoryNode } from '@/types/inventory'
import { MonitoredState, Tag } from '@/types/graphql'
import { useTagStore } from '../Components/tagStore'

export const useInventoryStore = defineStore('inventoryStore', {
  state: () => ({
    isTagManagerOpen: false,
    isTagManagerReset: false,
    isFilterOpen: false,
    monitoredFilterActive: false,
    unmonitoredFilterActive: false,
    detectedFilterActive: false,
    nodesSelected: [] as InventoryNode[],
    selectedMonitoredState: MonitoredState.Monitored,
    searchVariables: {
      searchType: { id: 1, name: 'Labels' },
      labelSearchTerm: '',
      tags: []
    },
    tagsSelected: [] as Tag[],
    isEditMode: false
  }),
  actions: {
    toggleTagManager() {
      this.isTagManagerOpen = !this.isTagManagerOpen

      if (!this.isTagManagerOpen) {
        this.tagsSelected = []
        this.isEditMode = false
        const tagStore = useTagStore()
        tagStore.tagsSelected = []
        tagStore.setTagEditMode(false)
      }
    },
    toggleFilter() {
      this.isFilterOpen = !this.isFilterOpen
    },
    toggleNodeEditMode() {
      this.isEditMode = !this.isEditMode
    },
    addSelectedTag(beep: Tag[]) {
      this.tagsSelected = beep
    },
    resetNodeEditMode() {
      this.isEditMode = false
    },
    addRemoveNodesSelected(node: InventoryNode) {
      if (this.nodesSelected.find((d) => d.id === node.id)) {
        this.nodesSelected = this.nodesSelected.filter(({ id }) => id !== node.id);
      } else {
        this.nodesSelected.push(node)
      }
    },
    selectAll(allNodes: InventoryNode[]) {
      this.nodesSelected = [...allNodes]
    },
    clearSelectedNodes() {
      this.nodesSelected = []
    },
    resetSearch() {
      this.searchVariables = {
        searchType: { id: 1, name: 'Labels' },
        labelSearchTerm: '',
        tags: []
      }
    },
    resetSelectedNode() {
      this.nodesSelected = []
    }
  }
})
