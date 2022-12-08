import NetworkGraph from '@/components//Topology/NetworkGraph.vue'
import setupWrapper from 'tests/setupWrapper'

const wrapper = setupWrapper({
  component: NetworkGraph,
  props: {
    refresh: () => ''
  }
})

test('The Network Graph mounts', () => {
  expect(wrapper).toBeTruthy()
})
