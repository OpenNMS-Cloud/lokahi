import { mount } from '@vue/test-utils'
import HeadlineSection from '@/components/Common/HeadlineSection.vue'

const defaultHeading = 'Section Headline'
const text = 'New Headline'

let wrapper: any

describe('HeadLineSection', () => {
  afterAll(() => {
    wrapper.unmount()
  })

  it(`should have ${defaultHeading} as heading`, () => {
    wrapper = mount(HeadlineSection)

    const componentHeading = wrapper.get('[data-test="section-headline"]').text()
    expect(componentHeading).toBe(defaultHeading)
  })

  it(`should have ${text} as heading`, () => {
    wrapper = mount(HeadlineSection, {
      propsData: {
        text
      }
    })

    const componentHeading = wrapper.get('[data-test="section-headline"]').text()
    expect(componentHeading).toBe(text)
  })

  it('should have rendered the slots', () => {
    wrapper = mount(HeadlineSection, {
      slots: {
        infos: '<div>some infos</div>',
        actions: '<div>some actions</div>'
      },
      attachTo: document.body
    })

    let slot = wrapper.get('[data-test="infos-slot"]')
    expect(slot.html()).toContain('some infos')

    slot = wrapper.get('[data-test="actions-slot"]')
    expect(slot.html()).toContain('some actions')
  })
})
