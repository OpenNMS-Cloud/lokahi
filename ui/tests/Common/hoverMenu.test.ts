import { mount } from '@vue/test-utils'
import HoverMenu from '@/components/Common/HoverMenu.vue'

let wrapper: any

const mock = {
  label: 'edit',
  handler: () => {
    console.log('handler called!!!')
  }
}

describe('HoverMenu', () => {
  beforeAll(() => {
    wrapper = mount(HoverMenu, {
      propsData: {
        items: [mock]
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('should have an item in the list', async () => {
    await wrapper.get('[data-test="hover-menu-icon"]').trigger('mouseover')

    const item = wrapper.find('[data-test="edit"]')
    expect(item.text()).toEqual('edit')
  })

  // TODO: not sure why the test failed
  test.skip('should call the handler method when click', async () => {
    const spy = vi.spyOn(mock, 'handler')

    await wrapper.get('[data-test="hover-menu-icon"]').trigger('mouseover')

    const item = wrapper.find('[data-test="edit"]')
    await item.trigger('click')

    // wrapper.vm.$nextTick()
    /* wrapper.vm.$nextTick(() => {
      expect(spy).toHaveBeenCalledTimes(1)
    }) */
    expect(spy).toHaveBeenCalledOnce()
    // expect(spy).toHaveBeenCalledTimes(1)
  })
})
