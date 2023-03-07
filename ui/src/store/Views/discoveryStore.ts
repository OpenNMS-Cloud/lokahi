import { DiscoveryInput } from '@/types/discovery'
import { defineStore } from 'pinia'
import { useDiscoveryMutations } from '../Mutations/discoveryMutations'
import { cloneDeep } from 'lodash'
import { DiscoveryType } from '@/components/Discovery/discovery.constants'
import { ActiveDiscovery, PassiveDiscovery } from '@/types/graphql'

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
    selectedTags: [] as Record<string, string>[],
    ipAddresses: <string[]>[],
    ipRange: {
      cidr: '',
      fromIp: '',
      toIp: ''
    },
    tags: [] as Record<string, string>[],
    udpPorts: [] as number[],
    communiyString: [] as string[],
    activeDiscoveries: <DiscoveryInput[]>[],
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
    selectTags(tags: Record<string, string>[]) {
      this.selectedTags = tags
    },
    async saveDiscoveryAzure() {
      const { addAzureCreds, azureError } = useDiscoveryMutations()

      await addAzureCreds({
        azureCredential: {
          location: this.selectedLocations[0],
          tags: this.selectedTags,
          ...this.azure
        }
      })
      return !azureError.value
    },
    clearAzureForm() {
      this.azure = cloneDeep(defaultAzureForm)
    },

    setSelectedDiscovery(selected: ActiveDiscovery | PassiveDiscovery | null) {
      if (!selected) {
        this.selectedDiscovery = Object.assign({ type: DiscoveryType.None })
        this.clearAzureForm()
      } else {
        // add check type
        const discovery = cloneDeep(selected)
        if (discovery) {
          // Todo: add type guards
          // this.ipAddresses = discovery.ipAddresses || []
          // this.udpPorts = discovery.snmpConfig?.ports || []
        }
      }
    }
  }
})
