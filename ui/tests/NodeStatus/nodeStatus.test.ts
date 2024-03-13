import NodeStatus from '@/containers/NodeStatus.vue'
import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import { createRouter, createWebHistory } from 'vue-router'

let wrapper: any
let router: any

describe('Node Status page', () => {
  afterAll(async () => {
    if (wrapper) {
      await wrapper.unmount()
    }
  })

  beforeEach(async () => {
    router = createRouter({
      history: createWebHistory(),
      routes: []
    })

    router.currentRoute.value.params = { id: '1' }
    router.push('/')
    await router.isReady()

    wrapper = mountWithPiniaVillus({
      component: NodeStatus,
      global: {
        plugins: [router]

      },
      shallow: true
    })
  })

  it('should have the required components', async () => {
    const pageHeader = await wrapper.get('[data-test="title"]')
    expect(pageHeader.exists()).toBe(true)
  })
})
