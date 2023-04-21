import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { NetworkTrafficDocument, TimeRangeUnit, TsData } from '@/types/graphql'

export const useDashboardQueries = defineStore('dashboardQueries', () => {
  const totalNetworkTrafficIn = ref([] as TsData)
  const totalNetworkTrafficOut = ref([] as TsData)

  const metricsQuery = ref({
    name: 'total_network_bytes_in',
    timeRange: 24,
    timeRangeUnit: TimeRangeUnit.Hour
  })

  const { data: networkTrafficData, execute: getMetrics } = useQuery({
    query: NetworkTrafficDocument,
    variables: metricsQuery
  })

  const getNetworkTrafficInMetrics = async () => {
    metricsQuery.value.name = 'total_network_bytes_in'
    await getMetrics()
    totalNetworkTrafficIn.value = (networkTrafficData.value || []) as TsData
  }

  const getNetworkTrafficOutMetrics = async () => {
    metricsQuery.value.name = 'total_network_bytes_out'
    await getMetrics()
    totalNetworkTrafficOut.value = (networkTrafficData.value || []) as TsData
  }

  return {
    getNetworkTrafficInMetrics,
    getNetworkTrafficOutMetrics,
    networkTrafficIn: computed(() => totalNetworkTrafficIn.value),
    networkTrafficOut: computed(() => totalNetworkTrafficOut.value)
  }
})
