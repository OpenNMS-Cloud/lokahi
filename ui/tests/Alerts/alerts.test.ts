import mount from '../mountWithPiniaVillus'
import Alerts from '@/containers/Alerts.vue'
import { useAlertsStore } from '@/store/Views/alertsStore'

let wrapper: any

describe('Alerts', () => {
  afterAll(() => {
    wrapper.unmount()
  })

  describe('Required elements', () => {
    beforeAll(() => {
      wrapper = mount({
        component: Alerts
      })
    })

    test('Mount', () => {
      expect(wrapper).toBeTruthy()
    })

    test('Should have headline', () => {
      const elem = wrapper.get('[data-test="headline"]')
      expect(elem.exists()).toBeTruthy()
    })

    test('Should have clear all filters button', async () => {
      const btn = wrapper.get('[data-test="clear-all-filters-btn"]')
      expect(btn.exists()).toBeTruthy()

      // TODO: not working
      // const alertsStore = useAlertsStore()
      // const spy = vi.spyOn(alertsStore, 'clearAllFilters')
      // await btn.trigger('click')
      // expect(spy).toHaveBeenCalled()
    })

    test('Should have severity filters', () => {
      const elem = wrapper.get('[data-test="severity-filters"]')
      expect(elem.exists()).toBeTruthy()
    })
  })

  describe('Alerts list not empty', () => {
    beforeAll(async () => {
      wrapper = mount({
        component: Alerts
      })

      const alertsStore = useAlertsStore()
      alertsStore.isAlertsListEmpty = false
      await wrapper.vm.$nextTick()
    })

    test('Should have time filters', async () => {
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


    test('Should have list count', async () => {
      const elem = wrapper.find('[data-test="list-count"]')
      expect(elem.exists()).toBeTruthy()
    })

    test('Should have alerts list', () => {
      const elem = wrapper.get('[data-test="alerts-list"]')
      expect(elem.exists()).toBeTruthy()
    })

    test('Should have pagination', async () => {
      const elem = wrapper.find('[data-test="pagination"]')
      expect(elem.exists()).toBeTruthy()
    })
  })

  describe('Alerts list empty', () => {
    beforeAll(() => {
      wrapper = mount({
        component: Alerts
      })
    })

    test('Should not have list count', async () => {
      const elem = wrapper.find('[data-test="list-count"]')
      expect(elem.exists()).toBeFalsy()
    })

    test('Should not have pagination', () => {
      const elem = wrapper.find('[data-test="pagination"]')
      expect(elem.exists()).toBeFalsy()
    })
  })
})
