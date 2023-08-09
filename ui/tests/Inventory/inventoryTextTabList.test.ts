import { mount } from '@vue/test-utils'
import InventoryTextTagList from '@/components/Inventory/InventoryTextTagList.vue'

let wrapper: any

describe('inventoryTextAnchorList', () => {
  beforeAll(() => {
    wrapper = mount(InventoryTextTagList, {
      shallow: true,
      props: {
        ipAddress: '',
        location: ''
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  const anchorList = ['location', 'management-ip']
  it.each(anchorList)('should have "%s" element', (elem) => {
    expect(wrapper.get(`[data-test="${elem}"]`).exists()).toBe(true)
  })
})
