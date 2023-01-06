import DiscoveryStepper from '@/components/Discovery/DiscoveryStepper.vue'
import DiscoveryStep1 from '@/components/Discovery/DiscoveryStep1.vue'
import mount from 'tests/mountWithPiniaVillus'

const wrapper = mount({ 
  component: DiscoveryStepper,
  attachTo: document.body,
  shallow: true
})

const step1Wrapper = mount({ 
  component: DiscoveryStep1 
})

test('DiscoveryStepper mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})

test('Step 1 mounts correctly', () => {
  expect(step1Wrapper).toBeTruthy()
})
