import { defineStore } from 'pinia'
import { TimeRange } from '@/types/graphql'
import { useAlertsQueries } from '../Queries/alertsQueries'
import { useAlertsMutations } from '../Mutations/alertsMutations'
import { Alert } from '@/types/graphql'
import { AlertsFilters } from '@/types/alerts'

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
  const alertsFilter = ref(alertsFilterDefault)
  const alertsPagination = ref(alertsPaginationDefault)
  const alertsSelected = ref([] as number[] | undefined)
  const isAlertsListEmpty = ref(true)
  const gotoFirstPage = ref(false)
  const alertsListSearched = ref([]) // TODO: not avail for EAR

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

  watch(
    alertsFilter,
    (newFilter, oldFilter) => {
      if (newFilter.severities !== oldFilter.severities || newFilter.timeRange !== oldFilter.timeRange) {
        gotoFirstPage.value = true
      }

      fetchAlerts()
    },
    { deep: true }
  )

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

    alertsPagination.value = {
      ...alertsPagination.value,
      page: 1 // always request first page on change
    }
  }

  const selectTime = (selected: TimeRange): void => {
    alertsFilter.value = {
      ...alertsFilter.value,
      timeRange: selected
    }
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
    alertsFilter.value = alertsFilterDefault
    alertsPagination.value = alertsPaginationDefault
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
    isAlertsListEmpty,
    gotoFirstPage
  }
})
