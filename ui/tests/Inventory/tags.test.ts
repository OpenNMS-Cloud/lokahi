import TagManager from '@/components/Inventory/TagManager.vue'
import TagManagerCtrl from '@/components/Inventory/TagManagerCtrl.vue'
import setupWrapper from 'tests/setupWrapper'

const tagManagerWrapper = setupWrapper({ 
  component: TagManager 
})

const tagManagerCtrlWrapper = setupWrapper({ 
  component: TagManagerCtrl 
})

test('The tags manager component mounts', () => {
  expect(tagManagerWrapper).toBeTruthy()
})

test('The tags manager ctrl component mounts', () => {
  expect(tagManagerCtrlWrapper).toBeTruthy()
})
