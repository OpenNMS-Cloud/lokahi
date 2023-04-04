import { mount } from '@vue/test-utils'
import AlertsSeverityLabel from '@/components/Alerts/AlertsSeverityLabel.vue'

let wrapper: any

describe('AlertsSeverityLabel', () => {
  beforeAll(() => {
    wrapper = mount(AlertsSeverityLabel, {
      props: {
        severity: 'CRITICAL'
      }
    })
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('Should have a severity label', () => {
    const elem = wrapper.get('[data-test="severity-status"]')
    expect(elem.exists()).toBeTruthy()
  })
})
