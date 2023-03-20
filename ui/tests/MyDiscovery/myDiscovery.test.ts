import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import MyDiscovery from '@/containers/MyDiscovery.vue'

const wrapper = mountWithPiniaVillus({
  component: MyDiscovery
})

test('The MyDiscovery page container mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})
