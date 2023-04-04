import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { AlertsDocument, CountAlertsDocument, TimeRange } from '@/types/graphql'
import { AlertsFilters } from '@/types/alerts'

export const useAlertsQueries = defineStore('alertsQueries', () => {
  const fetchAlertsData = ref({})

  const fetchAlerts = async (alertsFilters: AlertsFilters) => {
    const { data, execute, isFetching } = useQuery({
      query: AlertsDocument,
      variables: {
        page: alertsFilters.pagination.page,
        pageSize: alertsFilters.pagination.pageSize,
        search: alertsFilters.search,
        severities: alertsFilters.severities,
        sortAscending: alertsFilters.sortAscending,
        sortBy: alertsFilters.sortBy,
        timeRange: alertsFilters.timeRange
      },
      fetchOnMount: false,
      cachePolicy: 'network-only'
    })

    await execute()

    if (!isFetching.value) {
      fetchAlertsData.value = data.value?.findAllAlerts || []
    }
  }

  const fetchCountAlerts = async (severityFilters = [] as string[], timeRange = TimeRange.All) =>
    useQuery({
      query: CountAlertsDocument,
      variables: {
        severityFilters,
        timeRange
      },
      fetchOnMount: false,
      cachePolicy: 'network-only'
    })

  return {
    fetchAlerts,
    fetchAlertsData,
    fetchCountAlerts
  }
})
