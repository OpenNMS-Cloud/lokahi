import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import {
  AlertCountsDocument,
  AlertsListDocument,
  CountAlertsDocument,
  ListAlertResponse,
  TimeRange
} from '@/types/graphql'
import { AlertsFilters, Pagination } from '@/types/alerts'

export const defaultListAlertResponse = () => {
  return {
    alerts: [],
    lastPage: 0,
    nextPage: 0,
    totalAlerts: 0
  } as ListAlertResponse
}

export const useAlertsQueries = defineStore('alertsQueries', () => {
  const fetchAlertsData = ref({} as ListAlertResponse)
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

    if (data.value?.findAllAlerts) {
      fetchAlertsData.value = { ...data.value.findAllAlerts } as ListAlertResponse
    } else {
      fetchAlertsData.value = defaultListAlertResponse()
    }
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

  const getCounts = async () => {
    const { execute, data } = useQuery({
      query: AlertCountsDocument,
      cachePolicy: 'network-only',
      fetchOnMount: false
    })
    await execute()
    return data.value?.alertCounts ?? {}
  }

  return {
    getCounts,
    fetchAlerts,
    fetchAlertsData,
    fetchCountAlerts,
    fetchCountAlertsData
  }
})
