import mount from '../mountWithPiniaVillus'
import AlertsSeverityFilters from '@/components/Alerts/AlertsSeverityFilters.vue'

let wrapper: any

describe.skip('AlertsSeverityFilters', () => {
  beforeEach(() => {
    wrapper = mount({
      component: AlertsSeverityFilters
    })
  })
  afterEach(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })
})
