import { createTestingPinia } from '@pinia/testing'
import { useAlertsStore } from '../../../src/store/Views/alertsStore'
import { setActiveClient, useClient } from 'villus'


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
})
