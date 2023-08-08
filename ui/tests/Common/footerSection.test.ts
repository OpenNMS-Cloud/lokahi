import { mount } from '@vue/test-utils'
import FooterSection from '@/components/Common/FooterSection.vue'

let wrapper: any

describe('FooterSection', () => {
  beforeAll(() => {
    wrapper = mount(FooterSection, {
      slots: {
        buttons: '<div data-test="save-button">save btn</div> <div data-test="cancel-button">cancel btn</div>'
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('Should have a save button', () => {
    const elem = wrapper.get('[data-test="save-button"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a cancel button', () => {
    const elem = wrapper.get('[data-test="cancel-button"]')
    expect(elem.exists()).toBeTruthy()
  })
})
