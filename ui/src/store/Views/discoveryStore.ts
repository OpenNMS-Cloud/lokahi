import { DiscoveryInput } from '@/types/discovery'
import { defineStore } from 'pinia'
import { useDiscoveryMutations } from '../Mutations/discoveryMutations'
import { cloneDeep } from 'lodash'
import { DiscoveryType } from '@/components/Discovery/discovery.constants'
import { DiscoveryConfig } from '@/types/graphql'

const defaultAzureForm = {
  name: '',
  clientId: '',
  clientSecret: '',
  subscriptionId: '',
  directoryId: ''
}

const defaultSnmpForm = {
  id: 0,
  name: '',
  location: [],
  type: DiscoveryType.ICMP
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
    snmp: cloneDeep(defaultSnmpForm),
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
    setTags(tags: Record<string, string>[]) {
      this.tags = tags
    },
    setIpAddresses(ips: string[]) {
      this.ipAddresses = ips
    },
    setUdpPorts(ports: number[]) {
      this.udpPorts = ports
    },
    setCommunityString(str: string[]) {
      this.communiyString = str
    },
    async saveDiscoverySnmp() {
      const { createDiscoveryConfig, errorSnmp } = useDiscoveryMutations()
      await createDiscoveryConfig({
        snmpInfo: {
          configName: this.snmp.name,
          // tags: this.tags,
          ipAddresses: this.ipAddresses,
          location: this.selectedLocations[0],
          snmpConfig: { readCommunities: this.communiyString, ports: this.udpPorts }
        }
      })
      return !errorSnmp.value
    },
    clearSnmpForm() {
      this.snmp = cloneDeep(defaultSnmpForm)
    },
    setSelectedDiscovery(selected: DiscoveryConfig | null) {
      if (!selected) {
        this.selectedDiscovery = Object.assign({ type: DiscoveryType.None })
        this.clearAzureForm()
        this.clearSnmpForm()
      } else {
        // add check type
        const discovery = cloneDeep(selected)
        if (discovery) {
          this.snmp.name = discovery.configName || ''
          this.ipAddresses = discovery.ipAddresses || []
          this.udpPorts = discovery.snmpConfig?.ports || []
        }
      }
    }
  }
})
