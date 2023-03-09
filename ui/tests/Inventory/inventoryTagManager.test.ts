import mount from '../mountWithPiniaVillus'
import InventoryTagManager from '@/components/Inventory/InventoryTagManager.vue'
import { useInventoryStore } from '@/store/Views/inventoryStore'
import { useTagStore } from '@/store/Components/tagStore'

let wrapper: any

describe('InventoryTagManager.vue', () => {
  beforeAll(() => {
    wrapper = mount({
      component: InventoryTagManager
    })

    const inventoryStore = useInventoryStore()
    inventoryStore.isTagManagerOpen = true

    const tagStore = useTagStore()
    tagStore.tags = [{ id: 1, name: 'tag1' }]
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  describe('Required elements', () => {
    const elems = [
      ['Total', 'total'],
      ['Selected', 'selected'],
      ['Select/Deselect all', 'select-deselect-all'],
      ['Tags search autocomplete', 'tags-autocomplete'],
      ['Tag list', 'tag-chip-list'],
      ['Save button', 'save-btn'],
      ['Cancel button', 'cancel-btn']
    ]
    test.each(elems)('Should have "%s" element', (_, dataTest) => {
      const elem = wrapper.get(`[data-test="${dataTest}"]`)
      expect(elem.exists()).toBeTruthy()
    })
  })
})
