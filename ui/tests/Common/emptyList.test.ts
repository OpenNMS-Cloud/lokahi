import { shallowMount } from '@vue/test-utils'
import EmptyList from '@/components/Common/EmptyList.vue'

const mock = {
  msg: 'A given message.',
  btn: {
    label: 'button label',
    action: () => ({})
  }
}

let wrapper: any

describe('EmptyList', () => {
  beforeAll(() => {
    wrapper = shallowMount(EmptyList, {
      props: {
        content: mock
      }
    })
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('Should have a message', () => {
    const elem = wrapper.get('[data-test="msg]').text()
    expect(elem).toEqual(mock.msg)
  })

  test('Should have a button', async () => {
    const btn = wrapper.get('[data-test="btn"]')
    expect(btn.exists()).toBeTruthy()
  })
})
