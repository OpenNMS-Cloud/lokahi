import Discovery from '@/containers/Discovery.vue'
import mount from 'tests/mountWithPiniaVillus'
import router from '@/router'


let wrapper: any
describe('DiscoveryPage', () => {
  beforeAll(() => {
    wrapper = mount({
      component: Discovery,
      global: {
        plugins: [router]
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })
   
  test('The Discovery page container mounts correctly', () => {
    expect(wrapper).toBeTruthy()
  })

})
