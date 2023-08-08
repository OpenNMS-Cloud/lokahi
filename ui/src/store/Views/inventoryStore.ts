import { defineStore } from 'pinia'
import { InventoryItem, InventoryPage, NewInventoryNode, RawMetrics } from '@/types/inventory'
import { Tag } from '@/types/graphql'
import { useTagStore } from '../Components/tagStore'
import { useInventoryQueries } from '../Queries/inventoryQueries'
import { InventoryMapper } from '@/mappers'


export const useInventoryStore = defineStore('inventoryStore', {
  state: () => ({
    isTagManagerOpen: false,
    isTagManagerReset: false,
    isFilterOpen: false,
    loading: false,
    monitoredFilterActive: false,
    nodes: [] as InventoryItem[],
    unmonitoredFilterActive: false,
    detectedFilterActive: false,
    nodesSelected: [] as NewInventoryNode[],
    searchType: { id: 1, name: 'Labels' },
    tagsSelected: [] as Tag[],
    loadingTimeout: -1,
    isEditMode: false
  }),
  actions: {
    init(){
      this.loading = true
      const {buildNetworkInventory, receivedNetworkInventory} = useInventoryQueries()
      receivedNetworkInventory(this.receivedNetworkInventory as any)
      buildNetworkInventory()
      this.loadingTimeout = window.setTimeout(() => {this.loading = false},3000)
    },
    receivedNetworkInventory(d: {findAllNodes:Array<NewInventoryNode>,allMetrics: RawMetrics}) {
      const b= InventoryMapper.fromServer(d.findAllNodes,d.allMetrics)
      this.nodes = b.nodes
      window.clearTimeout(this.loadingTimeout)
      this.loading = false
    },
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
    addRemoveNodesSelected(node: NewInventoryNode) {
      if (this.nodesSelected.find((d) => d.id === node.id)) {
        this.nodesSelected = this.nodesSelected.filter(({ id }) => id !== node.id)
      } else {
        this.nodesSelected.push(node)
      }
    },
    selectAll(allNodes: NewInventoryNode[]) {
      this.nodesSelected = [...allNodes]
    },
    clearAll() {
      this.nodesSelected = []
    },
    setSearchType(searchType: { id: number, name: string }) {
      this.searchType = searchType
    },
    resetSelectedNode() {
      this.nodesSelected = []
    }
  }
})
