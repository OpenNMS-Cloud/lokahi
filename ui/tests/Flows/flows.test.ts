import Flows from '@/containers/Flows.vue'
import setupWrapper from 'tests/setupWrapper'

const wrapper = setupWrapper({
  component: Flows
})

test('The Flows page container mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})

test('The Flows page title should be Flows', () => {
  const title = wrapper.get('[data-test="flows-page-header"]')
  expect(title.text()).toBe('Flows')
})
