import mount from '../mountWithPiniaVillus'
import InventoryTagManager from '@/components/Inventory/InventoryTagManager.vue'
import { useInventoryStore } from '@/store/Views/inventoryStore'

let wrapper: any

describe('Tag manager', () => {
  beforeAll(() => {
    wrapper = mount({
      component: InventoryTagManager,
      shalow: true
    })
  })

  test('Mount component', () => {
    expect(wrapper.exists()).toBeTruthy()
  })

  test('Should have the required sections', () => {
    const inventoryStore = useInventoryStore()
    inventoryStore.isTaggingBoxOpen = true

    const selectTags = wrapper.get('[data-testid]="select-tags"')
    expect(selectTags.exists()).toBeTruthy()

    const tagNodes = wrapper.get('[data-testid]="tag-nodes"')
    expect(tagNodes.exists()).toBeTruthy()
  })
})
