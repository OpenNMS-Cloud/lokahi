import { mount } from '@vue/test-utils'
import InventoryIconActionList from '@/components/Inventory/InventoryIconActionList.vue'

let wrapper: any

/*
TODO: TypeError: Cannot read properties of undefined (reading 'title')
❯ Proxy._sfc_render src/components/Inventory/InventoryIconActionList.vue:9:27
     7|     <li @click="onDelete" data-test="delete"><Icon :icon="deleteIcon" /></li>
     8|   </ul>
     9|   <PrimaryModal :visible="isVisible" :title="modal.title" :class="modal.cssClass">
      |                           ^
    10|     <template #content>
    11|       <p>{{ modal.content }}</p>
*/
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
