import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import Flows from '@/containers/Flows.vue'

const wrapper = mountWithPiniaVillus({
  component: Flows,
  shallow: false
})

test('The Flows page container mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})

test('The Flows page title should be Flows', () => {
  const title = wrapper.get('[data-test="flows-page-header"]')
  expect(title.text()).toBe('Flows')
})
