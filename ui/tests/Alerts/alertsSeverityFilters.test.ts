import mount from '../mountWithPiniaVillus'
import AlertsSeverityFilters from '@/components/Alerts/AlertsSeverityFilters.vue'

let wrapper: any

describe('AlertsSeverityFilters', () => {
  beforeEach(() => {
    wrapper = mount({
      component: AlertsSeverityFilters,
      props: {
        isFilter: true
      }
    })
  })
  afterEach(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })
})
