import mount from '../mountWithPiniaVillus'
import LocationsCard from '@/components/Locations/LocationsCard.vue'

const mock = {
  location: 'Default',
  status: 'WARNING',
  contextMenu: [
    { label: 'edit', handler: () => ({}) },
    { label: 'delete', handler: () => ({}) }
  ]
}

let wrapper: any

describe('LocationsCard', () => {
  beforeAll(() => {
    wrapper = mount({
      component: LocationsCard,
      props: {
        item: mock
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('Should have a text button', () => {
    const elem = wrapper.get('[data-test="locationNameButton"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a context menu', () => {
    const elem = wrapper.get('[data-test="context-menu"]')
    expect(elem.exists()).toBeTruthy()
  })
})
