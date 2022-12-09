import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import { 
  ListNodesForTableDocument, 
  ListMinionsForTableDocument, 
  ListMinionsAndDevicesForTablesDocument, 
  ListMinionsForTableQuery, 
  ListNodesForTableQuery,
  ListMinionMetricsDocument,
  TsResult
} from '@/types/graphql'
import { ExtendedMinion } from '@/types/minion'
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

    const fetchMinionMetrics = (instance: string) => useQuery({
      query: ListMinionMetricsDocument,
      variables: { instance },
      cachePolicy: 'network-only'
    })
    
    const addMetricsToMinions = (resp: Ref<ListMinionsForTableQuery | null>)=> {
      const minions = resp.value?.findAllMinions as ExtendedMinion[] || []
      
      minions.forEach(async (minion) => {
        const { data, isFetching } = await fetchMinionMetrics(minion.systemId as string)
        
        if (data.value && !isFetching.value) {
          const [{ value }] = data.value.minionLatency?.data?.result as TsResult[]
          const [, val] = value as number[]

          minion.icmp_latency = val
        }
      })
    
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

    const locations = computed(() => minionsAndNodes.value?.findAllLocations || [])
    
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
