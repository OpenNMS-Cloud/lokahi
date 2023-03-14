import { defineStore } from 'pinia'
import { useDiscoveryMutations } from '../Mutations/discoveryMutations'
import { cloneDeep } from 'lodash'
import { TagCreateInput } from '@/types/graphql'

const defaultAzureForm = {
  name: '',
  clientId: '',
  clientSecret: '',
  subscriptionId: '',
  directoryId: ''
}

export const useDiscoveryStore = defineStore('discoveryStore', {
  state: () => ({
    selectedLocations: <string[]>[],
    selectedTags: [] as TagCreateInput[],
    ipAddresses: <string[]>[],
    ipRange: {
      cidr: '',
      fromIp: '',
      toIp: ''
    },
    tags: [] as Record<string, string>[],
    udpPorts: [] as number[],
    communiyString: [] as string[],
    azure: cloneDeep(defaultAzureForm),
    selectedDiscovery: {}
  }),
  actions: {
    selectLocation(location: string, single?: boolean) {
      if (single) {
        this.selectedLocations = location ? [location] : []
        return
      }

      if (this.selectedLocations.includes(location)) {
        this.selectedLocations = this.selectedLocations.filter((x) => x !== location)
      } else {
        this.selectedLocations.push(location)
      }
    },
    selectTags(tags: TagCreateInput[]) {
      this.selectedTags = tags
    },
    async saveDiscoveryAzure() {
      const { addAzureCreds, azureError } = useDiscoveryMutations()

      await addAzureCreds({
        discovery: {
          location: this.selectedLocations[0],
          tags: this.selectedTags,
          ...this.azure
        }
      })
      return !azureError.value
    },
    clearAzureForm() {
      this.azure = cloneDeep(defaultAzureForm)
    }
  }
})
