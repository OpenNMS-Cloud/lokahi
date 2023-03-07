import { mount } from '@vue/test-utils'
import BasicAutocomplete from '@/components/Common/BasicAutocomplete.vue'

let wrapper: any

describe('BasicAutocomplete', () => {
  beforeAll(() => {
    wrapper = mount(BasicAutocomplete, { shallow: true, props: { label: '', getItems: () => ({}), items: [] } })
  })

  test('Mount component', () => {
    const cmp = wrapper.get('[data-test="fds-autocomplete"]')
    expect(cmp.exists()).toBeTruthy()
  })

  test('Should have a chip list (type: single - default)', () => {
    const list = wrapper.find('[data-test="fds-chip-list"]')
    expect(list.exists()).toBeTruthy()
  })
})
