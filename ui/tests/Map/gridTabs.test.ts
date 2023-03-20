import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import GridTabs from '@/components/Map/GridTabs.vue'
import router from '@/router'

let wrapper: any

beforeEach(() => {
  wrapper = mountWithPiniaVillus({
    component: GridTabs,
    shallow: false,
    global: {
      plugins: [router]
    }
  })
})

it('should redirect to the alarm list', async () => {
  const push = vi.spyOn(router, 'push')

  await wrapper.get('[data-test="alarm-tab"]').trigger('click')

  expect(push).toHaveBeenCalledTimes(1)
  expect(push).toHaveBeenCalledWith('/map')
})
it('should redirect to the alarm of a node', async () => {
  const push = vi.spyOn(router, 'push')

  router.push('/map?nodeId=1234')
  expect(push).toHaveBeenCalledTimes(1)

  await wrapper.get('[data-test="alarm-tab"]').trigger('click')
  expect(push).toHaveBeenCalledTimes(2)
  expect(push).toHaveBeenCalledWith('/map?nodeId=1234')
})

it('should redirect to the node list', async () => {
  const push = vi.spyOn(router, 'push')

  await wrapper.get('[data-test="nodes-tab"]').trigger('click')

  expect(push).toHaveBeenCalledTimes(1)
  expect(push).toHaveBeenCalledWith('/map/nodes')
})
