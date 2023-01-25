import DiscoveryStepper from '@/components/Discovery/DiscoveryStepper.vue'
import DiscoveryStep1 from '@/components/Discovery/DiscoveryStep1.vue'
import mount from 'tests/mountWithPiniaVillus'
import setupWrapper from 'tests/setupWrapper'

const wrapper = setupWrapper({ 
  component: DiscoveryStepper,
  attachTo: document.body,
  type: 'shallow',
  props: {
    callback: () => {}
  }
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
