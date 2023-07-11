import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { AlertsListDocument, CountAlertsDocument, TimeRange } from '@/types/graphql'
import { AlertsFilters, Pagination } from '@/types/alerts'

export const useAlertsQueries = defineStore('alertsQueries', () => {
  const fetchAlertsData = ref({})
  const fetchCountAlertsData = ref(0)

  const fetchAlerts = async (alertsFilters: AlertsFilters, pagination: Pagination) => {
    const { data, execute } = useQuery({
      query: AlertsListDocument,
      variables: {
        page: pagination.page,
        pageSize: pagination.pageSize,
        severities: alertsFilters.severities,
        sortAscending: alertsFilters.sortAscending,
        sortBy: alertsFilters.sortBy,
        timeRange: alertsFilters.timeRange,
        nodeLabel: alertsFilters.nodeLabel
      },
      fetchOnMount: false,
      cachePolicy: 'network-only'
    })

    await execute()

    fetchAlertsData.value = data.value?.findAllAlerts || {}
  }

  const fetchCountAlerts = async (severityFilters = [] as string[], timeRange = TimeRange.All) => {
    const { data, execute } = useQuery({
      query: CountAlertsDocument,
      variables: {
        severityFilters,
        timeRange
      },
      fetchOnMount: false,
      cachePolicy: 'network-only'
    })

    await execute()

    fetchCountAlertsData.value = data.value?.countAlerts?.count || 0
  }

  return {
    fetchAlerts,
    fetchAlertsData,
    fetchCountAlerts,
    fetchCountAlertsData
  }
})
