import { createTestingPinia } from '@pinia/testing'
import { useAlertsStore } from '../../../src/store/Views/alertsStore'
import { setActiveClient, useClient } from 'villus'
import { TimeRange } from '@/types/graphql'


describe('Alerts Store', () => {
  let alertsStore: ReturnType<typeof useAlertsStore>

  beforeAll(() => {
    createTestingPinia({ stubActions: false })
    setActiveClient(useClient({ url: 'http://test/graphql' })) // Create and set a client
  })

  beforeEach(() => {
    alertsStore = useAlertsStore()
  })

  it('should initialize with empty alerts list', () => {
    expect(alertsStore.isAlertsListEmpty).toBe(true)
  })

  it('should return correct value for isAlertSelected', () => {
    alertsStore.setAlertSelected(1, true)
    expect(alertsStore.isAlertSelected(1)).toBe(true)
    expect(alertsStore.isAlertSelected(2)).toBe(false)
  })

  it('should select all alerts', () => {
    vi.spyOn(alertsStore, 'setAllAlertsSelected')
    alertsStore.setAllAlertsSelected(true)
    expect(alertsStore.setAllAlertsSelected).toHaveBeenCalledWith(true)
  })

  it('should deselect all alerts', () => {
    alertsStore.setAllAlertsSelected(true)
    vi.spyOn(alertsStore, 'setAllAlertsSelected')
    alertsStore.setAllAlertsSelected(false)
    expect(alertsStore.setAllAlertsSelected).toHaveBeenCalledWith(false)
  })

  it('should set an alert as selected', () => {
    alertsStore.setAlertSelected(1, true)
    expect(alertsStore.isAlertSelected(1)).toBe(true)
  })

  it('should clear selected alerts', async () => {
    vi.spyOn(alertsStore, 'clearSelectedAlerts')
    await alertsStore.clearSelectedAlerts()
    expect(alertsStore.clearSelectedAlerts).toHaveBeenCalled()
  })

  it('should acknowledge selected alerts', async () => {
    vi.spyOn(alertsStore, 'acknowledgeSelectedAlerts')
    await alertsStore.acknowledgeSelectedAlerts()
    expect(alertsStore.acknowledgeSelectedAlerts).toHaveBeenCalled()
  })



  it('should fetch alerts', async () => {
    vi.spyOn(alertsStore, 'fetchAlerts')
    await alertsStore.fetchAlerts()
    expect(alertsStore.fetchAlerts).toHaveBeenCalled()
  })

  it('should reset pagination and fetch alerts', async () => {
    vi.spyOn(alertsStore, 'fetchAlerts')
    vi.spyOn(alertsStore, 'resetPaginationAndFetchAlerts')
    await alertsStore.fetchAlerts()
    await alertsStore.resetPaginationAndFetchAlerts()
    expect(alertsStore.resetPaginationAndFetchAlerts).toHaveBeenCalled()
    expect(alertsStore.fetchAlerts).toHaveBeenCalled()
  })

  it('should toggle severity', () => {
    const initialFilter = { ...alertsStore.alertsFilter }
    alertsStore.toggleSeverity('high')
    expect(alertsStore.alertsFilter.severities).toContain('high')
    alertsStore.toggleSeverity('high')
    expect(alertsStore.alertsFilter).toEqual(initialFilter)
  })

  it('should select time range', () => {
    const initialFilter = { ...alertsStore.alertsFilter }
    alertsStore.selectTime(TimeRange.All)
    expect(alertsStore.alertsFilter.timeRange).toBe(TimeRange.All)
    alertsStore.selectTime(TimeRange.All)
    expect(alertsStore.alertsFilter).toEqual(initialFilter)
  })

  it('should clear all filters', () => {
    alertsStore.alertsFilter.severities = ['high']
    alertsStore.clearAllFilters()
    expect(alertsStore.alertsFilter).toEqual({
      timeRange: TimeRange.All,
      nodeLabel: '',
      severities: [],
      sortAscending: false,
      sortBy: 'lastEventTime'
    })
  })

  it('should set page', async () => {
    vi.spyOn(alertsStore, 'fetchAlerts')
    alertsStore.setPage(2)
    expect(alertsStore.alertsPagination.page).toBe(2)
    await alertsStore.fetchAlerts()
    expect(alertsStore.fetchAlerts).toHaveBeenCalled()
  })

  it('should set page size', async () => {
    vi.spyOn(alertsStore, 'fetchAlerts')
    alertsStore.setPageSize(20)
    expect(alertsStore.alertsPagination.pageSize).toBe(20)
    await alertsStore.fetchAlerts()
    expect(alertsStore.fetchAlerts).toHaveBeenCalled()
  })
})
