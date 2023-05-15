import { mount } from '@vue/test-utils'
import AlertsCard from '@/components/Alerts/AlertsCard.vue'
import { getAlert } from '../fixture/alerts'

let wrapper: any

describe('Alert card', () => {
  beforeAll(() => {
    wrapper = mount(AlertsCard, {
      props: {
        alert: getAlert()
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('Should have required elements', () => {
    const elems = ['checkbox', 'name', 'severity', 'cause', 'duration', 'date', 'time', 'check-icon']
    elems.forEach((elem) => {
      const el = wrapper.get(`[data-test="${elem}"]`)
      expect(el.exists()).toBeTruthy()
    })
  })

  test('Should have description when card expanded', async () => {
    const ahref = wrapper.get('.feather-expansion-header-button')
    expect(ahref.exists()).toBeTruthy()

    await ahref.trigger('click')
    const elem = wrapper.get('[data-test="description"]')
    expect(elem.exists()).toBeTruthy()
  })
})
