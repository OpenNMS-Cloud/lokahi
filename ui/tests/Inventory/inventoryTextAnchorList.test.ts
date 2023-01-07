import { mount } from '@vue/test-utils'
import InventoryTextAnchorList from '@/components/Inventory/InventoryTextAnchorList.vue'

let wrapper: any

// TODO: will fix after demo
describe.skip('inventoryTextAnchorList', () => {
  beforeAll(() => {
    wrapper = mount(InventoryTextAnchorList, {
      shallow: true,
      props: {
        anchor: {
          profileValue: 75,
          profileLink: '#',
          locationValue: 'DefaultMinion',
          locationLink: '#',
          managementIpValue: '0.0.0.0',
          managementIpLink: '#',
          tagValue: 100,
          tagLink: '#'
        }
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  const anchorList = ['profile', 'location', 'management-ip', 'tag']
  it.each(anchorList)('should have "%s" element', (elem) => {
    expect(wrapper.get(`[data-test="${elem}"]`).exists()).toBe(true)
  })
})
