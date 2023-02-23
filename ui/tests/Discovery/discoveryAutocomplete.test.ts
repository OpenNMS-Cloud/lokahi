import { mount } from '@vue/test-utils'
import DiscoveryAutocomplete from '@/components/Discovery/DiscoveryAutocomplete.vue'

let wrapper: any

describe('DiscoveryAutocomplete', () => {
  beforeAll(() => {
    wrapper = mount(DiscoveryAutocomplete, { shallow: true, props: { label: '', getItems: () => ({}), items: [] } })
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
