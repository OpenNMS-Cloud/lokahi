import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import { ListNodesForTableDocument, ListMinionsForTableDocument, ListMinionsAndDevicesForTablesDocument, ListMinionsForTableQuery, ListNodesForTableQuery } from '@/types/graphql'
import { ExtendedMinionDTO } from '@/types/minion'
import { ExtendedNode } from '@/types/node'
import { Ref } from 'vue'

export const useAppliancesQueries = defineStore('appliancesQueries', {
  state: () => {
    const tableNodes = ref()
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
    
    // fetch appliance nodes table data
    const fetchNodesForTable = () => {
      const { data: nodesData } = useQuery({
        query: ListNodesForTableDocument,
        cachePolicy: 'network-only'
      })

      watchEffect(() => {
        tableNodes.value = addMetricsToNodes(nodesData)
      })
    }

    const addMetricsToNodes = (data: Ref<ListNodesForTableQuery | null>)=> {
      const nodes = data.value?.findAllNodes as ExtendedNode[] || []
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

      // TODO: add metrics when available
      // for (const node of nodes) {
      //   node.icmp_latency = latenciesMap[node.managementIp as string]
      //   node.snmp_uptime = uptimesMap[node.managementIp as string]
      //   node.status = (node.icmp_latency >= 0 && node.snmp_uptime >= 0) ? 'UP' : 'DOWN'
      // }
      
      return nodes
    }

    // minions AND nodes table
    const { data: minionsAndNodes, execute } = useQuery({
      query: ListMinionsAndDevicesForTablesDocument,
      cachePolicy: 'network-only'
    })

    watchEffect(() => {
      tableMinions.value = addMetricsToMinions(minionsAndNodes)
      tableNodes.value = addMetricsToNodes(minionsAndNodes)
    })

    const locations = computed(() => minionsAndNodes.value?.listLocations?.locations?.map((item, index) => ({ id: index, name: item.locationName })) || [])
    
    return {
      tableMinions,
      fetchMinionsForTable,
      tableNodes,
      fetchNodesForTable,
      locations,
      fetch: execute
    }
  }
})
