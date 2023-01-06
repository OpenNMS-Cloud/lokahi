import Discovery from '@/containers/Discovery.vue'
import mount from 'tests/mountWithPiniaVillus'

const wrapper = mount({ 
  component: Discovery,
  attachTo: document.body,
  shallow: true
})

test('The Discovery page container mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})
