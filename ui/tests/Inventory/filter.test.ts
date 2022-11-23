import { shallowMount } from '@vue/test-utils'
import Filter from '@/components/Inventory/Filter.vue'

let wrapper: any

describe('Filter component', () => {
  beforeAll(() => {
    wrapper = shallowMount(Filter)
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
      'tagging',
      'sort-icon',
      'sort-alpha-icon',
      'expand-icon'
      // 'collapse-icon'
    ]

    it.each(requiredComponents)('should have "%s" component', (item) => {
      expect(wrapper.getComponent(`[data-test="${item}"]`).exists()).toBe(true)
    })

    it('should have "collapse-icon" component', async () => {
      await wrapper.findComponent('[data-test="expand-icon"]').trigger('click')
      
      const collapseIcon = wrapper.findComponent('[data-test="collapse-icon"]') 
      expect(collapseIcon.exists()).toBe(true)
    })
  })
})