import { mount } from '@vue/test-utils'
import InventoryTextTagList from '@/components/Inventory/InventoryTextTagList.vue'

let wrapper: any

describe('inventoryTextAnchorList', () => {
  beforeAll(() => {
    wrapper = mount(InventoryTextTagList, {
      shallow: true,
      props: {
        anchor: {
          profileValue: 75,
          profileLink: '#',
          locationValue: 'DefaultMinion',
          locationLink: '#',
          managementIpValue: '0.0.0.0',
          managementIpLink: '#',
          tagValue: []
        }
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
