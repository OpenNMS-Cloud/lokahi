import { defineStore } from 'pinia'
import { useDiscoveryMutations } from '../Mutations/discoveryMutations'
import { cloneDeep } from 'lodash'
import { MonitoringLocation, TagCreateInput } from '@/types/graphql'

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
    selectLocation(location: MonitoringLocation, single?: boolean) {
      if (single) {
        this.selectedLocations = location ? [location.id] : []
        return
      }

      if (this.selectedLocations.includes(location.id)) {
        this.selectedLocations = this.selectedLocations.filter((x) => x !== location.id)
      } else {
        this.selectedLocations.push(location.id)
      }
    },
    selectTags(tags: TagCreateInput[]) {
      this.selectedTags = tags
    },
    async saveDiscoveryAzure() {
      const { addAzureCreds, azureError } = useDiscoveryMutations()

      await addAzureCreds({
        discovery: {
          locationId: this.selectedLocations[0],
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
