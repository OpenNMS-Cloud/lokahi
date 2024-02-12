import { mount } from '@vue/test-utils'
import LogoDarkIcon from '@/components/Common/LogoDarkIcon.vue'

let wrapper: any

describe('LogoDarkIcon', () => {
  beforeAll(() => {
    wrapper = mount(LogoDarkIcon, { shallow: true })
  })

  test('Mount component', () => {
    const cmp = wrapper.get('[data-test="logo-dark-icon"]')
    expect(cmp.exists()).toBeTruthy()
  })
})
