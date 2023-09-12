import Discovery from '@/containers/Discovery.vue'
import mount from 'tests/mountWithPiniaVillus'
import router from '@/router'
import { useDiscoveryStore } from '@/store/Views/discoveryStore'

let wrapper: any
describe('DiscoveryPage', () => {
  beforeAll(() => {
    wrapper = mount({
      component: Discovery,
      global: {
        plugins: [router]
      },
      stubActions: false
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  test('The Discovery page container mounts correctly', () => {
    expect(wrapper).toBeTruthy()
  })

  test('Store fn backToDiscovery', () => {
    const store = useDiscoveryStore()

    store.newDiscoveryModalActive = true
    store.validationErrors = { name: 'test' }
    store.selectedDiscovery = { name: 'Azure' }

    store.backToDiscovery()

    expect(store.newDiscoveryModalActive).toBe(false)
    expect(JSON.stringify(store.validationErrors)).toBe('{}')
    expect(JSON.stringify(store.selectedDiscovery)).toBe('{}')
  })

  test('Store fn startNewDiscovery', () => {
    const store = useDiscoveryStore()
    store.startNewDiscovery()
    expect(store.discoveryTypePageActive).toBe(true)
    expect(store.setupDefaultDiscovery).toHaveBeenCalledOnce()
  })
})
