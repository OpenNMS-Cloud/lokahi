import shallowMount from 'tests/mountWithPiniaVillus'
import AlertsCardList from '@/components/Alerts/AlertsCardList.vue'
import { getAlertsList } from '../fixture/alerts'

let wrapper: any

describe('Alerts list', () => {
  afterAll(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    wrapper = shallowMount({
      component: AlertsCardList,
      props: {
        alerts: []
      }
    })

    expect(wrapper).toBeTruthy()
  })

  test('Should have alerts list if list not empty', async () => {
    wrapper = shallowMount({
      component: AlertsCardList,
      props: {
        alerts: getAlertsList()
      }
    })

    const elem = wrapper.find('[data-test="alerts-list"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should not have alerts list if list empty', () => {
    wrapper = shallowMount({
      component: AlertsCardList,
      props: {
        alerts: []
      }
    })
    const elem = wrapper.find('[data-test="empty-list"]')
    expect(elem.exists()).toBeTruthy()
  })
})
