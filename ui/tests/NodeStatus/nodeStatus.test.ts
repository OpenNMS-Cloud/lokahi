import NodeStatus from '@/containers/NodeStatus.vue'
import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import router from '@/router'

let wrapper: any

describe('Node Status page', () => {
  afterAll(() => {
    wrapper.unmount()
  })
  beforeAll(() => {
    wrapper = mountWithPiniaVillus({
      component: NodeStatus,
      global: {
        plugins: [router]

      },
      shallow: true
    })
  })
  it('should have the required components', () => {
    const pageHeader = wrapper.get('[data-test="title"]')
    expect(pageHeader.exists()).toBe(true)

  })
})
