import mount from '../mountWithPiniaVillus'
import Alerts from '@/containers/Alerts.vue'
import { useAlertsStore } from '@/store/Views/alertsStore'
import { getAlertsList } from '../../mock-graphql/src/fixture/alerts.fixture'

let wrapper: any

describe('Alerts', () => {
  beforeEach(() => {
    wrapper = mount({
      component: Alerts,
      shallow: false
    })
  })
  afterEach(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('Should have headline', () => {
    const elem = wrapper.get('[data-test="headline"]')
    expect(elem.exists()).toBeTruthy()
    expect(elem.text()).toBe('Alerts')
  })

  test('Should have clear all filters button', async () => {
    const alertsStore = useAlertsStore()
    const spy = vi.spyOn(alertsStore, 'clearAllFilters')

    const btn = wrapper.get('[data-test="clear-all-filters-btn"]')
    expect(btn.exists()).toBeTruthy()

    // TODO: not working
    // await btn.trigger('click')
    // expect(spy).toHaveBeenCalled()
  })

  test('Should have severity filters', () => {
    const elem = wrapper.get('[data-test="severity-filters"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have time filters', () => {
    const elem = wrapper.get('[data-test="time-filters"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have search filter', () => {
    const elem = wrapper.get('[data-test="search-filter"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have select all checkbox', () => {
    const checkbox = wrapper.get('[data-test="select-all-checkbox"]')
    expect(checkbox.exists()).toBeTruthy()
  })

  test('Should have clear button', () => {
    const btn = wrapper.get('[data-test="clear-btn"]')
    expect(btn.exists()).toBeTruthy()
  })

  test('Should have acknowledge button', () => {
    const btn = wrapper.get('[data-test="acknowledge-btn"]')
    expect(btn.exists()).toBeTruthy()
  })

  test('Should not have list count', async () => {
    const elem = wrapper.find('[data-test="list-count"]')
    expect(elem.exists()).toBeFalsy()
  })

  test('Should have list count', async () => {
    const alertsStore = useAlertsStore()
    alertsStore.alertsList = getAlertsList()
    await wrapper.vm.$nextTick()

    const elem = wrapper.find('[data-test="list-count"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have alerts list', () => {
    const elem = wrapper.get('[data-test="alerts-list"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should not have pagination', () => {
    const elem = wrapper.find('[data-test="pagination"]')
    expect(elem.exists()).toBeFalsy()
  })

  test('Should have pagination', async () => {
    const alertsStore = useAlertsStore()
    alertsStore.alertsList = getAlertsList()
    await wrapper.vm.$nextTick()

    const elem = wrapper.find('[data-test="pagination"]')
    expect(elem.exists()).toBeTruthy()
  })
})
