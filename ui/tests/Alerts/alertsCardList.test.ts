import mount from 'tests/mountWithPiniaVillus'
import AlertsCardList from '@/components/Alerts/AlertsCardList.vue'
import { useAlertsStore } from '@/store/Views/alertsStore'
import { getAlertsList } from '../../mock-graphql/src/fixture/alerts.fixture'

let wrapper: any

describe.skip('Alerts list', () => {
  beforeEach(() => {
    wrapper = mount({
      component: AlertsCardList
    })
  })
  afterEach(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('Should have alerts list if list not empty', async () => {
    const alertsStore = useAlertsStore()
    alertsStore.alertsList = getAlertsList()
    await wrapper.vm.$nextTick()

    const elem = wrapper.find('[data-test="alerts-list"]')
    expect(elem.exists()).toBeTruthy()
  })

  describe('Alerts list empty', () => {
    test('Should not have alerts list if list empty', () => {
      const elem = wrapper.find('[data-test="empty-list"]')
      expect(elem.exists()).toBeTruthy()
    })

    test('Should have a message', () => {
      const elem = wrapper.get('[data-test="msg"]')
      expect(elem.exists()).toBeTruthy()
    })

    test('Should have clear all filters button', async () => {
      const btn = wrapper.find('[data-test="clear-all-filters-btn"]')
      expect(btn.exists()).toBeTruthy()

      // TODO not working
      // const alertsStore = useAlertsStore()
      // const spy = vi.spyOn(alertsStore, 'clearAllFilters')
      // await btn.trigger('click')
      // expect(spy).toHaveBeenCalled()
    })
  })
})
