import Dashboard from '@/containers/Dashboard.vue'
import setupWrapper from 'tests/setupWrapper'

const wrapper = setupWrapper({ 
  component: Dashboard,
  shallow: true
})

test('The Dashboard page container mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})
