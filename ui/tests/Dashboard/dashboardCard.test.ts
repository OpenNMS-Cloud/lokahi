import DashboardCard from '@/components/Dashboard/DashboardCard.vue'
import dashboardTexts from '@/components/Dashboard/dashboard.text'
import mount from 'tests/mountWithPiniaVillus'
import router from '@/router'

let wrapper: any

describe('Dashboard Card component', () => {
  beforeAll(() => {
    wrapper = mount({
      component: DashboardCard,
      shallow: false,
      props: {
        texts: dashboardTexts.NetworkTraffic,
        redirectLink: 'Inventory'
      },
      global: {
        plugins: [router]
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  it('The DashboardCard component mounts correctly', () => {
    expect(wrapper).toBeTruthy()
  })

  it('should redirect to the Inventory Page', async () => {
    const push = vi.spyOn(router, 'push')
    await wrapper.get('[data-test="redirect-link"]').trigger('click')
    expect(push).toHaveBeenCalledTimes(1)
    expect(push).toHaveBeenCalledWith('Inventory')
  })
})
