import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import Dashboard from '@/containers/Dashboard.vue'
import router from '@/router'

const wrapper = mountWithPiniaVillus({
  component: Dashboard,
  shallow: true,
  global: {
    plugins: [router]
  }
})

test('The Dashboard page container mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})
