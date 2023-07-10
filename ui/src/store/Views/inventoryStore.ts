import { defineStore } from 'pinia'
import { InventoryNode } from '@/types/inventory'
import { Tag } from '@/types/graphql'
import { useTagStore } from '../Components/tagStore'

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

      if (!this.isTagManagerOpen) {
        this.tagsSelected = [];
        this.isEditMode = false;
        const tagStore = useTagStore();
        tagStore.tagsSelected = [];
        tagStore.setTagEditMode(false);
      }
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
    clearAll() {
      this.nodesSelected = [];
    },
    setSearchType(searchType: { id: number, name: string }) {
      this.searchType = searchType;
    },
    resetSelectedNode() {
      this.nodesSelected = []
    }
  }
})
