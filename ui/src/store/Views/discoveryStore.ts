import { defineStore } from 'pinia'
import { useDiscoveryMutations } from '../Mutations/discoveryMutations'
import { cloneDeep } from 'lodash'

const defaultAzureForm = {
  name: '',
  clientId: '',
  clientSecret: '',
  subscriptionId: '',
  directoryId: '',
  tags: []
}

export const useDiscoveryStore = defineStore('discoveryStore', {
  state: () => ({
    selectedLocations: <string[]>[],
    ipAddresses: <string[]>[],
    ipRange: {
      cidr: '',
      fromIp: '',
      toIp: ''
    },
    azure: cloneDeep(defaultAzureForm)
  }),
  actions: {
    selectLocation(location: string, single?: boolean) {
      if (single) {
        this.selectedLocations = [location]
        return
      }

      if (this.selectedLocations.includes(location)) {
        this.selectedLocations = this.selectedLocations.filter((x) => x !== location)
      } else {
        this.selectedLocations.push(location)
      }
    },
    async saveDiscoveryAzure() {
      const { addAzureCreds, azureError } = useDiscoveryMutations()

      await addAzureCreds({
        azureCredential: {
          location: this.selectedLocations[0],
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
