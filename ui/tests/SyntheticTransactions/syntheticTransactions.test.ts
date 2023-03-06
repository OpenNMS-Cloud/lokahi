import SyntheticTransactions from '@/containers/SyntheticTransactions.vue'
import setupWrapper from 'tests/setupWrapper'

const wrapper = setupWrapper({ 
  component: SyntheticTransactions 
})

test('The Synthetic Transactions page container mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})
