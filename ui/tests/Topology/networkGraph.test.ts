import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import NetworkGraph from '@/components//Topology/NetworkGraph.vue'

const wrapper = mountWithPiniaVillus({
  component: NetworkGraph,
  props: {
    refresh: () => ''
  }
})

test('The Network Graph mounts', () => {
  expect(wrapper).toBeTruthy()
})
