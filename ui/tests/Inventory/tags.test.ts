import InventoryTagManager from '@/components/Inventory/InventoryTagManager.vue'
import InventoryTagManagerCtrl from '@/components/Inventory/InventoryTagManagerCtrl.vue'
import setupWrapper from 'tests/setupWrapper'

const tagManagerWrapper = setupWrapper({
  component: InventoryTagManager
})

const tagManagerCtrlWrapper = setupWrapper({
  component: InventoryTagManagerCtrl
})

test('The tags manager component mounts', () => {
  expect(tagManagerWrapper).toBeTruthy()
})

test('The tags manager ctrl component mounts', () => {
  expect(tagManagerCtrlWrapper).toBeTruthy()
})
