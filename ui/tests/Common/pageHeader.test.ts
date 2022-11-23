import { mount } from '@vue/test-utils'
import PageHeader from '@/components/Common/PageHeader.vue'

const defaultHeading = 'Page Heading'
const heading = 'New Heading'

let wrapper: any

describe('Page header component', () => {
  afterAll(() => {
    wrapper.unmount()
  })
  
  it(`should have ${defaultHeading} as heading`, () => {
    wrapper = mount(PageHeader)
    
    const componentHeading = wrapper.find('h2').text()
    expect(componentHeading).toBe(defaultHeading) 
  })
  it(`should have ${heading} as heading`, () => {
    wrapper = mount(PageHeader, {
      propsData: {
        heading
      }
    })
    
    const componentHeading = wrapper.find('h2').text()
    expect(componentHeading).toBe(heading) 
  })
})