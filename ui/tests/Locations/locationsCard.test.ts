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

describe.skip('LocationsCard', () => {
  beforeAll(() => {
    wrapper = mount({
      component: LocationsCard,
      propsData: {
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
    const elem = wrapper.get('[data-test="name"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a location name', () => {
    const elem = wrapper.get('[data-test="status"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should open location edit form', () => {
    expect(false).toBeTruthy()
  })

  test('Should have an icon', () => {
    const elem = wrapper.get('[data-test="icon-expiry"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a context menu', () => {
    const elem = wrapper.get('[data-test="context-menu"]')
    expect(elem.exists()).toBeTruthy()
  })
})
