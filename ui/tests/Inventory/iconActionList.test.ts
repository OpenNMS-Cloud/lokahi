import { mount } from '@vue/test-utils'
import IconActionList from '@/components/Inventory/IconActionList.vue'

let wrapper: any

describe('Inventory node icon action list', () => {
  beforeAll(() => {
    wrapper = mount(IconActionList, {
      shallow: true
    })
  })
  afterAll(() => {
    wrapper.unmount() 
  })

  const actionList = [
    'bubble-chart',
    'line-chart',
    'pie-chart',
    'warning',
    'delete'
  ]
  it.each(actionList)('should have "%s" action icon', (icon) => {
    expect(wrapper.get(`[data-test="${icon}"]`).exists()).toBe(true)
  })
})