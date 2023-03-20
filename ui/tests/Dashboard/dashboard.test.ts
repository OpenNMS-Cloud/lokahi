import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import Dashboard from '@/containers/Dashboard.vue'

const wrapper = mountWithPiniaVillus({
  component: Dashboard,
  shallow: true
})

test('The Dashboard page container mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})
