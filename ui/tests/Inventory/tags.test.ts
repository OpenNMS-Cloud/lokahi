import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import InventoryTagManager from '@/components/Inventory/InventoryTagManager.vue'
import InventoryTagManagerCtrl from '@/components/Inventory/InventoryTagManagerCtrl.vue'

const tagManagerWrapper = mountWithPiniaVillus({
  component: InventoryTagManager
})

const tagManagerCtrlWrapper = mountWithPiniaVillus({
  component: InventoryTagManagerCtrl
})

test('The tags manager component mounts', () => {
  expect(tagManagerWrapper).toBeTruthy()
})

test('The tags manager ctrl component mounts', () => {
  expect(tagManagerCtrlWrapper).toBeTruthy()
})
