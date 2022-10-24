import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import { ListDevicesForTableDocument, ListMinionsForTableDocument, ListMinionsAndDevicesForTablesDocument, ListMinionsForTableQuery, ListDevicesForTableQuery } from '@/types/graphql'
import { ExtendedMinionDTO } from '@/types/minion'
import { ExtendedDeviceDTO } from '@/types/device'
import { Ref } from 'vue'

export const useAppliancesQueries = defineStore('appliancesQueries', {
  state: () => {
    const tableDevices = ref()
    const tableMinions = ref()
    
    // fetch appliance minions table data
    const fetchMinionsForTable = () => {
      const { data: minionsData } = useQuery({
        query: ListMinionsForTableDocument,
        cachePolicy: 'network-only'
      })

      watchEffect(() => {
        tableMinions.value = addMetricsToMinions(minionsData)
      })
    }
    
    const addMetricsToMinions = (data: Ref<ListMinionsForTableQuery | null>)=> {
      const minions = data.value?.listMinions?.minions as ExtendedMinionDTO[] || []
      const minionLatencies = data.value?.minionLatency?.data?.result || []
      const minionUptimes = data.value?.minionUptime?.data?.result || []
    
      const latenciesMap: Record<string, number> = {}
      const uptimesMap: Record<string, number> = {}
    
      for (const latency of minionLatencies) {
        latenciesMap[latency?.metric?.instance] = latency?.value?.[1] || 0
      }
    
      for (const uptime of minionUptimes) {
        uptimesMap[uptime?.metric?.instance] = uptime?.value?.[1] || 0
      }
    
      for (const minion of minions) {
        minion.icmp_latency = latenciesMap[minion.id as string]
        minion.snmp_uptime = uptimesMap[minion.id as string]
      }
    
      return minions
    }
    
    // fetch appliance devices table data
    const fetchDevicesForTable = () => {
      const { data: devicesData } = useQuery({
        query: ListDevicesForTableDocument,
        cachePolicy: 'network-only' // always fetch and do not cache
      })

      watchEffect(() => {
        tableDevices.value = addMetricsToDevices(devicesData)
      })
    }

    const addMetricsToDevices = (data: Ref<ListDevicesForTableQuery | null>)=> {
      const devices = data.value?.listDevices?.devices as ExtendedDeviceDTO[] || []
      const deviceLatencies = data.value?.deviceLatency?.data?.result || []
      const deviceUptimes = data.value?.deviceUptime?.data?.result || []

      const latenciesMap: Record<string, number> = {}
      const uptimesMap: Record<string, number> = {}

      for (const latency of deviceLatencies) {
        latenciesMap[latency?.metric?.instance] = latency?.value?.[1] || 0
      }
      
      for (const uptime of deviceUptimes) {
        uptimesMap[uptime?.metric?.instance] = uptime?.value?.[1] || 0
      }

      for (const device of devices) {
        device.icmp_latency = latenciesMap[device.managementIp as string]
        device.snmp_uptime = uptimesMap[device.managementIp as string]
        device.status = (device.icmp_latency >= 0 && device.snmp_uptime >= 0) ? 'UP' : 'DOWN'
      }
      
      return devices
    }

    // minions AND devices table
    const { data: minionsAndDevices, execute } = useQuery({
      query: ListMinionsAndDevicesForTablesDocument,
      cachePolicy: 'network-only'
    })

    watchEffect(() => {
      tableMinions.value = addMetricsToMinions(minionsAndDevices)
      tableDevices.value = addMetricsToDevices(minionsAndDevices)
    })

    const locations = computed(() => minionsAndDevices.value?.listLocations?.locations?.map((item, index) => ({ id: index, name: item.locationName })) || [])
    
    return {
      tableMinions,
      fetchMinionsForTable,
      tableDevices,
      fetchDevicesForTable,
      locations,
      fetch: execute
    }
  }
})
