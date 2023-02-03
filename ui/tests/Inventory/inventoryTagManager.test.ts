import mount from '../mountWithPiniaVillus'
import InventoryTagManager from '@/components/Inventory/InventoryTagManager.vue'
import { useInventoryStore } from '@/store/Views/inventoryStore'

let wrapper: any

describe('Tag Manager', () => {
  beforeAll(() => {
    wrapper = mount({
      component: InventoryTagManager
    })

    const inventoryStore = useInventoryStore()
    inventoryStore.isTaggingBoxOpen = true
  })

  test('Mount', () => {
    expect(wrapper.exists()).toBeTruthy()
  })

  describe('Required elements', () => {
    const elems = [
      ['Heading', 'heading'],
      ['Total | Selected', 'total-selected'],
      ['Select tags', 'select-tags'],
      ['Search tags', 'search-tags'],
      ['Tag list', 'tags-list'],
      ['Tag nodes', 'tag-nodes']
    ]
    test.each(elems)('Should have "%s" element', (_, dataTest) => {
      const elem = wrapper.get(`[data-test="${dataTest}"]`)
      expect(elem.exists()).toBeTruthy()
    })
  })
})
