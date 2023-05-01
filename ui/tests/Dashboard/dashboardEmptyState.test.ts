import DashboardEmptyState from '@/components/Dashboard/DashboardEmptyState.vue'
import mount from 'tests/mountWithPiniaVillus'
import dashboardTexts from '@/components/Dashboard/dashboard.text'

let wrapper: any

describe('Dashboard EmptyState component', () => {
  beforeAll(() => {
    wrapper = mount({
      component: DashboardEmptyState,
      shallow: false,
      props: {
        texts: dashboardTexts.NetworkTraffic,
        redirectLink: 'Inventory'
      },
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  it('The DashboardEmptyState component mounts correctly', () => {
    expect(wrapper).toBeTruthy()
  })
})
