import { cloneDeep } from 'lodash'
import { defineStore } from 'pinia'
import { useAlertsMutations } from '../Mutations/alertsMutations'
import { defaultListAlertResponse, useAlertsQueries } from '../Queries/alertsQueries'
import { AlertsFilters, Pagination } from '@/types/alerts'
import { Alert, ListAlertResponse, TimeRange } from '@/types/graphql'

const alertsFilterDefault: AlertsFilters = {
  timeRange: TimeRange.All,
  nodeLabel: '',
  severities: [],
  sortAscending: false,
  sortBy: 'lastEventTime'
}

const alertsPaginationDefault: Pagination = {
  page: 1, // reset to first page. FE pagination component uses base 1 for page
  pageSize: 10,
  total: 0
}

export const useAlertsStore = defineStore('alertsStore', () => {
  // Current page of alert data
  const alertsList = ref<ListAlertResponse>(defaultListAlertResponse())

  const alertsFilter = ref(cloneDeep(alertsFilterDefault))
  const alertsPagination = ref(cloneDeep(alertsPaginationDefault))
  const alertsSelected = ref(new Set<number>())

  const alertsQueries = useAlertsQueries()
  const alertsMutations = useAlertsMutations()

  const isAlertsListEmpty = computed<boolean>(() => {
    const count = alertsList.value?.alerts?.length || 0
    return count < 1
  })

  const isAlertSelected = (id: number) => {
    return alertsSelected.value.has(id)
  }

  const fetchAlerts = async () => {
    // Api uses base 0 for page number; FE pagination uses base 1, so adjust here
    const page = alertsPagination.value.page > 0 ? alertsPagination.value.page - 1 : 0

    const pagination = {
      ...alertsPagination.value,
      page
    }

    await alertsQueries.fetchAlerts(alertsFilter.value, pagination)

    alertsList.value = alertsQueries.fetchAlertsData

    if (alertsList.value.totalAlerts != alertsPagination.value.total) {
      alertsPagination.value = {
        ...alertsPagination.value,
        total: alertsList.value.totalAlerts
      }
    }
  }

  const resetPaginationAndFetchAlerts = () => {
    alertsPagination.value.page = 1
    fetchAlerts().catch(() => 'Failed to fetch alerts')
  }

  const toggleSeverity = (selected: string): void => {
    const exists = alertsFilter.value.severities?.some((s) => s === selected)

    if (exists) {
      alertsFilter.value = {
        ...alertsFilter.value,
        severities: alertsFilter.value.severities?.filter((s) => s !== selected)
      }

      if (!alertsFilter.value.severities?.length)
        alertsFilter.value = {
          ...alertsFilter.value,
          severities: []
        }
    } else {
      alertsFilter.value = {
        ...alertsFilter.value,
        severities: [...(alertsFilter.value.severities as string[]), selected]
      }
    }

    resetPaginationAndFetchAlerts()
  }

  const selectTime = (selected: TimeRange): void => {
    alertsFilter.value = {
      ...alertsFilter.value,
      timeRange: selected
    }

    resetPaginationAndFetchAlerts()
  }

  const setPage = (page: number): void => {
    if (page !== Number(alertsPagination.value.page)) {
      alertsPagination.value = {
        ...alertsPagination.value,
        page
      }
    }
    fetchAlerts()
  }

  const setPageSize = (pageSize: number): void => {
    if (pageSize !== alertsPagination.value.pageSize) {
      alertsPagination.value = {
        ...alertsPagination.value,
        page: 1, // always request first page on change
        pageSize
      }
    }

    fetchAlerts()
  }

  const clearAllFilters = (): void => {
    alertsFilter.value = cloneDeep(alertsFilterDefault)
    alertsPagination.value = cloneDeep(alertsPaginationDefault)
    fetchAlerts()
  }

  /** Select or deselect all current alerts. */
  const setAllAlertsSelected = (isSelected: boolean) => {
    if (isSelected) {
      const ids: number[] = alertsList.value.alerts
        ?.map((al: Alert) => Number(al.databaseId))
        .filter((x: number) => !Number.isNaN(x) && x > 0) || []

      alertsSelected.value = new Set<number>(ids)
    } else {
      alertsSelected.value.clear()
    }
  }

  /** Select or deselect a single alert. */
  const setAlertSelected = (id: number, isSelected: boolean) => {
    if (alertsSelected.value.has(id)) {
      if (!isSelected) {
        alertsSelected.value.delete(id)
      }
    } else {
      if (isSelected) {
        alertsSelected.value.add(id)
      }
    }
  }

  const clearSelectedAlerts = async () => {
    await alertsMutations.clearAlerts({ ids: [...alertsSelected.value.values()] })

    fetchAlerts()
  }

  const acknowledgeSelectedAlerts = async () => {
    await alertsMutations.acknowledgeAlerts({ ids: [...alertsSelected.value.values()] })

    fetchAlerts()
  }

  return {
    acknowledgeSelectedAlerts,
    alertsFilter,
    alertsList,
    alertsPagination,
    clearAllFilters,
    clearSelectedAlerts,
    fetchAlerts,
    isAlertsListEmpty,
    isAlertSelected,
    resetPaginationAndFetchAlerts,
    selectTime,
    setAlertSelected,
    setAllAlertsSelected,
    setPageSize,
    setPage,
    toggleSeverity
  }
})
