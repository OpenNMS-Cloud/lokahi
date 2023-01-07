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
      'search',
      'node-type',
      'monitoring-location',
      'severity',
      'tag-manager-ctrl',
      'sort-btn',
      'sort-alpha-btn',
      'expand-btn'
      // 'collapse-btn'
    ]

    it.each(requiredComponents)('should have "%s" component', (item) => {
      expect(wrapper.get(`[data-test="${item}"]`).exists()).toBe(true)
    })

    it('should have collapse button', async () => {
      const inventoryStore = useInventoryStore()
      const toggleFilter = vi.spyOn(inventoryStore, 'toggleFilter')

      const expandBtn = wrapper.get('[data-test="expand-btn"]')
      await expandBtn.trigger('click')
      expect(toggleFilter).toHaveBeenCalledOnce()

      // TODO Assert collapse button after expand was clicked
      // const collapseBtn = wrapper.get('[data-test="collapse-btn"]')
      // expect(collapseBtn.existxs()).toBe(true)
    })
  })
})
