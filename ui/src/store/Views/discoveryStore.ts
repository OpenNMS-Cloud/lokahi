import { defineStore } from 'pinia'

export const useDiscoveryStore = defineStore('discoveryStore', {
  state: () => ({
    selectedLocationIds: <string[]>[],
    ipAddresses: <string[]>[],
    ipRange: {
      cidr: '',
      fromIp: '',
      toIp: ''
    }
  }),
  actions: {
    selectLocation(id: string) {
      if (this.selectedLocationIds.includes(id)) {
        this.selectedLocationIds = this.selectedLocationIds.filter(x => x !== id)
      } else {
        this.selectedLocationIds.push(id)
      }
    }
  }
})
