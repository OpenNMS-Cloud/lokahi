import DashboardNetworkTraffic from '@/components/Dashboard/DashboardNetworkTraffic.vue'
import mount from 'tests/mountWithPiniaVillus'

let wrapper: any

describe('DashboardNetworkTraffic component', () => {
  beforeAll(() => {
    wrapper = mount({
      component: DashboardNetworkTraffic,
      shallow: false
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  it('The DashboardNetworkTraffic component mounts correctly', () => {
    expect(wrapper).toBeTruthy()
  })
})
