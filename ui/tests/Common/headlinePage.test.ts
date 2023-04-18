import { mount } from '@vue/test-utils'
import HeadlinePage from '@/components/Common/HeadlinePage.vue'

const defaultHeading = 'Page Headline'
const text = 'New Headline'

let wrapper: any

describe('HeadlinePage', () => {
  afterAll(() => {
    wrapper.unmount()
  })

  it(`should have ${defaultHeading} as heading`, () => {
    wrapper = mount(HeadlinePage)

    const componentHeading = wrapper.get('[data-test="page-headline"]').text()
    expect(componentHeading).toBe(defaultHeading)
  })

  it(`should have ${text} as heading`, () => {
    wrapper = mount(HeadlinePage, {
      propsData: {
        text
      }
    })

    const componentHeading = wrapper.get('[data-test="page-headline"]').text()
    expect(componentHeading).toBe(text)
  })
})
