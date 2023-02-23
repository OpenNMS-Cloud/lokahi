import { mount } from '@vue/test-utils'
import DiscoveryContentEditable from '@/components/Discovery/DiscoveryContentEditable.vue'
import { ContentEditableType } from '@/components/Discovery/discovery.constants'

let wrapper: any

describe('DiscoveryContentEditable', () => {
  beforeEach(() => {
    const props = {
      contentType: ContentEditableType.IP,
      regexDelim: '[,; ]+'
    }

    wrapper = mount(DiscoveryContentEditable, {
      props
    })
  })

  test('Mount component', () => {
    expect(wrapper.exists()).toBeTruthy()
  })

  test('Should have a label', () => {
    const elem = wrapper.get('label')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have an input text', () => {
    const elem = wrapper.get('.content-editable')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should not have a validate icon', async () => {
    await wrapper.setProps({ regexDelim: '' })

    const elem = wrapper.get('.content-editable')
    elem.wrapperElement.textContent = ''
    await elem.trigger('keyup')

    const icon = wrapper.find('.validate-format')
    expect(icon.exists()).toBeFalsy()
  })

  test('Should have a validate icon', async () => {
    const elem = wrapper.get('.content-editable')
    elem.wrapperElement.textContent = 'some IP addresses'
    await elem.trigger('keyup')

    const icon = wrapper.get('.validate-format')
    expect(icon.exists()).toBeTruthy()
  })

  test('Should validate the text content', async () => {
    const fn = vi.spyOn(wrapper.vm, 'validateAndFormat')

    const elem = wrapper.get('.content-editable')
    elem.wrapperElement.textContent = 'some IP addresses'
    await elem.trigger('keyup')

    const icon = wrapper.get('.validate-format')
    await icon.trigger('click')

    expect(fn).toHaveBeenCalledOnce()
  })
})
