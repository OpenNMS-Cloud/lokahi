import mount from '../mountWithPiniaVillus'
import AlertsSeverityCard from '@/components/Alerts/AlertsSeverityCard.vue'
import { useAlertsStore } from '@/store/Views/alertsStore'
import { useAlertsQueries } from '@/store/Queries/alertsQueries'

let wrapper: any
let alertsQueries: any
let alertsStore: any

describe('AlertsSeverityCard', () => {
  beforeEach(() => {
    wrapper = mount({
      component: AlertsSeverityCard,
      props: {
        severity: 'CRITICAL',
        isFilter: true,
        count: 4
      }
    })
    alertsQueries = useAlertsQueries()
    alertsQueries.fetchCountAlertsData = 4
    alertsStore = useAlertsStore()
  })

  afterEach(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('Should have a severity card', () => {
    const elem = wrapper.get('[data-test="severity-card"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a severity label', () => {
    const elem = wrapper.get('[data-test="severity-label"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have an add/cancel icon', async () => {
    const elem = wrapper.get('[data-test="add-cancel-icon"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a count', () => {
    const elem = wrapper.get('[data-test="count"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should set severity filter when click on card', async () => {
    const spy = vi.spyOn(alertsStore, 'toggleSeverity')

    const elem = wrapper.get('[data-test="severity-card"]')
    await elem.trigger('click')

    expect(spy).toHaveBeenCalledWith('CRITICAL')
  })
})
