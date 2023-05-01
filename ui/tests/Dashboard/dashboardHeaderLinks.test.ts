import DashboardHeaderLinks from '@/components/Dashboard/DashboardHeaderLinks.vue'
import mount from 'tests/mountWithPiniaVillus'
import router from '@/router'

let wrapper: any

describe('DashboardHeaderLinks component', () => {
  beforeAll(() => {
    wrapper = mount({
      component: DashboardHeaderLinks,
      shallow: false,
      global: {
        plugins: [router]
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  it('The DashboardHeaderLinks component mounts correctly', () => {
    expect(wrapper).toBeTruthy()
  })
  
  it('Should redirect to Discovery', async () => {
    const push = vi.spyOn(router, 'push')
    await wrapper.get('[data-test="menu-dropdown"]').trigger('click')
    await wrapper.get('[data-test="menu-links"]').trigger('click')
    expect(push).toHaveBeenCalledTimes(1)
    expect(push).toHaveBeenCalledWith({name: 'Discovery'})
  })
})
