import { mount } from '@vue/test-utils'
import InventoryIconActionList from '@/components/Inventory/InventoryIconActionList.vue'

let wrapper: any

describe.skip('InventoryIconActionList.vue', () => {
  beforeAll(() => {
    wrapper = mount(InventoryIconActionList, {
      shallow: true
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  const actionList = [
    // 'bubble-chart',
    'line-chart',
    // 'pie-chart',
    'warning',
    'delete'
  ]
  it.each(actionList)('should have "%s" action icon', (icon) => {
    expect(wrapper.get(`[data-test="${icon}"]`).exists()).toBe(true)
  })
})
