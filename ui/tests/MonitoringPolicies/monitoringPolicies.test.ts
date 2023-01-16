import MonitoringPolicies from '@/containers/MonitoringPolicies.vue'
import mount from 'tests/mountWithPiniaVillus'

const wrapper = mount({ 
  component: MonitoringPolicies,
})

test('The Monitoring Policies page container mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})
