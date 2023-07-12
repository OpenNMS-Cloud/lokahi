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
    tagStore.tagsSelected = [{ id: 1, name: 'tag1' }]
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

})
