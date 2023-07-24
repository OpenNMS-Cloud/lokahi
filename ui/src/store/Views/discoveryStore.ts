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
    selectedLocation: undefined as undefined | MonitoringLocation,
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
    selectTags(tags: TagCreateInput[]) {
      this.selectedTags = tags
    },
    async saveDiscoveryAzure() {
      const { addAzureCreds, azureError } = useDiscoveryMutations()

      await addAzureCreds({
        discovery: {
          locationId: this.selectedLocation?.id,
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
