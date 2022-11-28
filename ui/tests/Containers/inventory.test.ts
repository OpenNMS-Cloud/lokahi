import { shallowMount } from '@vue/test-utils'
import Inventory from '@/containers/Inventory.vue'
import PageHeader from '@/components/Common/PageHeader.vue'
import { FeatherTabContainer } from '@featherds/tabs'

let wrapper: any

describe('Inventory page', () => {
  afterAll(() => {
    wrapper.unmount()
  })

  it('should have the required components', () => {
    wrapper = shallowMount(Inventory)
    
    const pageHeader = wrapper.getComponent(PageHeader)
    expect(pageHeader.exists()).toBe(true)
    
    const featherTabContainer = wrapper.getComponent(FeatherTabContainer)
    expect(featherTabContainer.exists()).toBe(true)
  })
})