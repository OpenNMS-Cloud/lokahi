import InventoryFilter from '@/components/Inventory/InventoryFilter.vue'
import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import { useInventoryStore } from '@/store/Views/inventoryStore'

let wrapper: any

describe('InventoryFilter.vue', () => {
  beforeAll(() => {
    wrapper = mountWithPiniaVillus({
      component: InventoryFilter,
      shallow: true
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  describe('Required components', () => {
    const requiredComponents = [
      'tag-manager-ctrl',
    ]

    it.each(requiredComponents)('should have "%s" component', (item) => {
      expect(wrapper.get(`[data-test="${item}"]`).exists()).toBe(true)
    })
  })
})
