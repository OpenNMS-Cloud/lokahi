import NodeStatus from '@/containers/NodeStatus.vue'
import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import { createRouter, createWebHistory } from 'vue-router'
import { RouterLinkStub } from '@vue/test-utils'

let wrapper: any
const mockRouter = createRouter({ history: createWebHistory(), routes: [ 
{  path: '/:pathMatch(.*)*',
  name: 'NotFoundPage',
  component: NodeStatus,
},         ] })
mockRouter.currentRoute.value.params = { id: '1' }
await mockRouter.isReady()
describe('Inventory page', () => {
  afterAll(() => {
    wrapper.unmount()
  })

  it('should have the required components', () => {
    wrapper = mountWithPiniaVillus({
      component: NodeStatus,
      global: {
        plugins: [mockRouter],
        stubs: {
          RouterLink: RouterLinkStub
        }
      },
      shallow: true
    })

    const pageHeader = wrapper.get('[data-test="title"]')
    expect(pageHeader.exists()).toBe(true)

    // const featherTabContainer = wrapper.getComponent('[data-test="tab-container"]')
    // expect(featherTabContainer.exists()).toBe(true)
  })
})
