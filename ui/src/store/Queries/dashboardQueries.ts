import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { NetworkTrafficDocument, TopNNodesDocument, TimeRangeUnit, TsData } from '@/types/graphql'

export const useDashboardQueries = defineStore('dashboardQueries', () => {
  const totalNetworkTrafficIn = ref([] as TsData)
  const totalNetworkTrafficOut = ref([] as TsData)

  const metricsQuery = ref({
    name: 'total_network_bits_in',
    timeRange: 24,
    timeRangeUnit: TimeRangeUnit.Hour
  })

  const topNNodesQuery = ref({
    timeRange: 24,
    timeRangeUnit: TimeRangeUnit.Hour
  })

  const { data: networkTrafficData, execute: getMetrics } = useQuery({
    query: NetworkTrafficDocument,
    variables: metricsQuery
  })

  const { data: topNodes, execute: getTopNodes } = useQuery({
    query: TopNNodesDocument,
    variables: topNNodesQuery,
    fetchOnMount: false
  })

  const getNetworkTrafficInMetrics = async () => {
    metricsQuery.value.name = 'total_network_bits_in'
    await getMetrics()
    totalNetworkTrafficIn.value = (networkTrafficData.value || []) as TsData
  }

  const getNetworkTrafficOutMetrics = async () => {
    metricsQuery.value.name = 'total_network_bits_out'
    await getMetrics()
    totalNetworkTrafficOut.value = (networkTrafficData.value || []) as TsData
  }

  return {
    getTopNodes,
    topNodes: computed(() => topNodes.value?.topNNode || []),
    getNetworkTrafficInMetrics,
    getNetworkTrafficOutMetrics,
    networkTrafficIn: computed(() => totalNetworkTrafficIn.value),
    networkTrafficOut: computed(() => totalNetworkTrafficOut.value)
  }
})
