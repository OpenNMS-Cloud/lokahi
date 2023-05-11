import { mount } from '@vue/test-utils'
import FooterSection from '@/components/Common/FooterSection.vue'

const mock = {
  save: { label: 'save', handler: () => ({}) },
  cancel: { label: 'cancel', handler: () => ({}) }
}

let wrapper: any

describe.skip('FooterSection', () => {
  beforeAll(() => {
    wrapper = mount(FooterSection, {
      propsData: {
        ...mock
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
