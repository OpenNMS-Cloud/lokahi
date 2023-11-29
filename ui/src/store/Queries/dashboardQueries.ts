import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import {
  NetworkTrafficDocument,
  TopNNodesDocument,
  TimeRangeUnit,
  TsData,
  TopNNodesQueryVariables,
  DownloadTopNQueryVariables,
  DownloadTopNDocument
} from '@/types/graphql'

export const useDashboardQueries = defineStore('dashboardQueries', () => {
  const totalNetworkTrafficIn = ref([] as TsData)
  const totalNetworkTrafficOut = ref([] as TsData)

  const metricsQuery = ref({
    name: 'total_network_bits_in',
    timeRange: 24,
    timeRangeUnit: TimeRangeUnit.Hour
  })

  const { data: networkTrafficData, execute: getMetrics } = useQuery({
    query: NetworkTrafficDocument,
    variables: metricsQuery
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

  const getTopNodes = async (topNNodesQueryVariables: TopNNodesQueryVariables) => {
    const { execute, data } = useQuery({
      query: TopNNodesDocument,
      variables: topNNodesQueryVariables,
      cachePolicy: 'network-only',
      fetchOnMount: false
    })
    await execute()
    return data.value
  }

  const downloadTopNodes = async (downloadTopNQueryVariables: DownloadTopNQueryVariables) => {
    const { execute, data } = useQuery({
      query: DownloadTopNDocument,
      variables: downloadTopNQueryVariables,
      cachePolicy: 'network-only',
      fetchOnMount: false
    })
    await execute()
    return data.value?.downloadTopN?.topNBytes
  }

  return {
    getTopNodes,
    downloadTopNodes,
    getNetworkTrafficInMetrics,
    getNetworkTrafficOutMetrics,
    networkTrafficIn: computed(() => totalNetworkTrafficIn.value),
    networkTrafficOut: computed(() => totalNetworkTrafficOut.value)
  }
})
