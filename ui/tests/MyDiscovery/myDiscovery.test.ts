import MyDiscovery from '@/containers/MyDiscovery.vue'
import setupWrapper from 'tests/setupWrapper'

const wrapper = setupWrapper({ 
  component: MyDiscovery 
})

test('The MyDiscovery page container mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})
