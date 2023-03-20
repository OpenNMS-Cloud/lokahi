import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import SyntheticTransactions from '@/containers/SyntheticTransactions.vue'

const wrapper = mountWithPiniaVillus({
  component: SyntheticTransactions
})

test('The Synthetic Transactions page container mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})
