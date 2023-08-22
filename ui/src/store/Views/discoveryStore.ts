import { defineStore } from 'pinia'
import { useDiscoveryMutations } from '../Mutations/discoveryMutations'
import { cloneDeep } from 'lodash'
import { AzureActiveDiscovery, IcmpActiveDiscovery, MonitoringLocation, PassiveDiscovery, TagCreateInput } from '@/types/graphql'
import { useDiscoveryQueries } from '../Queries/discoveryQueries'
import { DiscoveryType } from '@/components/Discovery/discovery.constants'

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
    deleteModalOpen: false,
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
    },
    upsertActiveDiscovery(discovery: PassiveDiscovery | AzureActiveDiscovery | IcmpActiveDiscovery | null){
      if (discovery){
        const discoveryMutations = useDiscoveryMutations()
        discoveryMutations.createOrUpdateDiscovery({request:discovery})
      }
    },
    openDeleteModal(){
      this.deleteModalOpen = true
    },
    async deleteDiscovery(id?: number, type?: string) {
      if (id){
        const discoveryMutations = useDiscoveryMutations()
        if (type === DiscoveryType.ICMP){
          await discoveryMutations.deleteActiveIcmpDiscovery({id})
        } else if (type === DiscoveryType.SyslogSNMPTraps){
          await discoveryMutations.deletePassiveDiscovery({id})
        }
        const discoveryQueries = useDiscoveryQueries()
        await discoveryQueries.getDiscoveries()
        this.deleteModalOpen = false
      }
    },
    closeDeleteModal(){
      this.deleteModalOpen = false
    }
  }
})
