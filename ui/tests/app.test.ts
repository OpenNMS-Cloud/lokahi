import { mount } from '@vue/test-utils'
import App from '@/App.vue'
import { createTestingPinia } from '@pinia/testing'
import { useWelcomeStore } from '@/store/Views/welcomeStore'

let wrapper: any

describe('AppTest', () => {
  beforeAll(() => {
    createTestingPinia()
    const welcomeStore = useWelcomeStore()
    welcomeStore.ready = true
    wrapper = mount(App, { shallow: true })
  })

  test('Mount component', () => {
    const cmp = wrapper.get('[data-test="main-content"]')
    expect(cmp.exists()).toBeTruthy()
  })
})
