import mount from '../mountWithPiniaVillus'
import AlertsSeverityCard from '@/components/Alerts/AlertsSeverityCard.vue'
import { useAlertsStore } from '@/store/Views/alertsStore'

let wrapper: any

describe('AlertsSeverityCard', () => {
  beforeEach(() => {
    wrapper = mount({
      component: AlertsSeverityCard,
      props: {
        severity: 'CRITICAL',
        count: 4
      }
    })
  })
  afterEach(() => {
    wrapper.unmount()
  })

  test.only('Mount', () => {
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

  test('Should have an add/cancel icon', () => {
    const elem = wrapper.get('[data-test="add-cancel-icon"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a count', () => {
    const elem = wrapper.get('[data-test="count"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a percentage/time', () => {
    const elem = wrapper.get('[data-test="percentage-time"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should set severity filter when click on card', async () => {
    const alertsStore = useAlertsStore()
    const spy = vi.spyOn(alertsStore, 'toggleSeverity')

    const elem = wrapper.get('[data-test="severity-card"]')
    await elem.trigger('click')

    expect(spy).toHaveBeenCalledWith('CRITICAL')
  })
})
