import { findByText } from 'tests/utils'
import mount from '../mountWithPiniaVillus'
import MoreOptionsMenu from '@/components/Common/MoreOptionsMenu.vue'

let wrapper: any

const mock = {
  label: 'edit',
  handler: () => {
    console.log('handler called!!!')
  }
}

describe('MoreOptionsMenu', () => {
  beforeAll(() => {
    wrapper = mount({
      component: MoreOptionsMenu,
      props: {
        items: [mock]
      },
      shallow: false,
      stubActions: false
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('should have an item in the list', async () => {
    await wrapper.get('[data-test="more-options-btn"]').trigger('click')

    const item = findByText(wrapper, 'div', 'edit')
    expect(item.text()).toEqual('edit')
  })
})
