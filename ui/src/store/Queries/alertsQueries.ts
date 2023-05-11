import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { AlertsListDocument, CountAlertsDocument, TimeRange } from '@/types/graphql'
import { AlertsFilters, Pagination } from '@/types/alerts'

export const useAlertsQueries = defineStore('alertsQueries', () => {
  const fetchAlertsData = ref({})

  const fetchAlerts = async (alertsFilters: AlertsFilters, pagination: Pagination) => {
    const { data, execute } = useQuery({
      query: AlertsListDocument,
      variables: {
        page: pagination.page,
        pageSize: pagination.pageSize,
        // search: alertsFilters.search, // TODO: not avail for EAR
        severities: alertsFilters.severities,
        sortAscending: alertsFilters.sortAscending,
        sortBy: alertsFilters.sortBy,
        timeRange: alertsFilters.timeRange
      },
      fetchOnMount: false,
      cachePolicy: 'network-only'
    })

    await execute()

    fetchAlertsData.value = data.value?.findAllAlerts || []
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
