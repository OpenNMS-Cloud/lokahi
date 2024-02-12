import mount from '../mountWithPiniaVillus'
import Welcome from '@/containers/Welcome.vue'
import router from '@/router'

let wrapper: any

describe('WelcomeGuide', () => {
  beforeAll(() => {
    wrapper = mount({
      component: Welcome,
      global: {
        plugins: [router]
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('Should have a logo', () => {
    const elem = wrapper.get('[data-test="welcome-logo"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a gradient background', () => {
    const elem = wrapper.get('[data-test="gradient-bg"]')
    expect(elem.exists()).toBeTruthy()
  })
})
