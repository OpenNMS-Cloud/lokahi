import { defineStore } from 'pinia'
import { TimeRange } from '@/types/graphql'
import { useAlertsQueries } from '../Queries/alertsQueries'
import { useAlertsMutations } from '../Mutations/alertsMutations'
import { Alert } from '@/types/graphql'
import { AlertsFilters } from '@/types/alerts'
import { cloneDeep } from 'lodash'

const alertsFilterDefault: AlertsFilters = {
  timeRange: TimeRange.All,
  nodeLabel: '',
  severities: [],
  sortAscending: true,
  sortBy: 'id'
}

const alertsPaginationDefault = {
  page: 1, // FE pagination component has base 1 (first page)
  pageSize: 10,
  total: 0
}

export const useAlertsStore = defineStore('alertsStore', () => {
  const alertsList = ref()
  const alertsFilter = ref(cloneDeep(alertsFilterDefault))
  const alertsPagination = ref(cloneDeep(alertsPaginationDefault))
  const alertsSelected = ref([] as number[] | undefined)
  const isAlertsListEmpty = ref(true)

  const alertsQueries = useAlertsQueries()
  const alertsMutations = useAlertsMutations()

  const fetchAlerts = async () => {
    alertsPagination.value = {
      ...alertsPagination.value,
      page: alertsPagination.value.page - 1 // AlertsList api has base 0 and FE pagination component has base 1; hence we always subtract 1 before sending request.
    }

    await alertsQueries.fetchAlerts(alertsFilter.value, alertsPagination.value)

    alertsList.value = alertsQueries.fetchAlertsData

    isAlertsListEmpty.value = Boolean(alertsList.value.alerts?.length <= 0)

    alertsPagination.value = {
      ...alertsPagination.value,
      total: alertsList.value.totalAlerts
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

  // all toggle (select/deselect): true/false / individual toggle: number (alert id)
  const setAlertsSelected = (selectedAlert: number | boolean) => {
    if (Number.isInteger(selectedAlert)) {
      const found = alertsSelected.value?.indexOf(selectedAlert as number) as number
      found === -1 ? alertsSelected.value?.push(selectedAlert as number) : alertsSelected.value?.splice(found, 1)
    } else {
      if (!selectedAlert) alertsSelected.value = []
      else {
        alertsSelected.value = alertsList.value.alerts.map((al: Alert) => al.databaseId)
      }
    }
  }

  const clearSelectedAlerts = async () => {
    await alertsMutations.clearAlerts({ ids: alertsSelected.value })

    fetchAlerts()
  }

  const acknowledgeSelectedAlerts = async () => {
    await alertsMutations.acknowledgeAlerts({ ids: alertsSelected.value })

    fetchAlerts()
  }

  return {
    alertsList,
    fetchAlerts,
    resetPaginationAndFetchAlerts,
    alertsFilter,
    toggleSeverity,
    selectTime,
    alertsPagination,
    setPage,
    setPageSize,
    clearAllFilters,
    setAlertsSelected,
    clearSelectedAlerts,
    acknowledgeSelectedAlerts,
    isAlertsListEmpty
  }
})
