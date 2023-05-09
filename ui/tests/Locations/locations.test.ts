import mount from '../mountWithPiniaVillus'
import Locations from '@/containers/Locations.vue'

let wrapper: any

describe.skip('Locations', () => {
  beforeAll(() => {
    wrapper = mount({
      component: Locations
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('Should have a headline', () => {
    const elem = wrapper.get('[data-test="locations-headline"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a notification ctrl btn', () => {
    const elem = wrapper.get('[data-test="locations-notification-ctrl"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a location add btn', () => {
    const elem = wrapper.get('[data-test="add-location-btn"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should open the location add form', async () => {
    await wrapper.get('[data-test="add-location-btn"]').trigger('click')

    const elem = wrapper.get('[data-test="location-add-form"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a location search input', () => {
    const elem = wrapper.get('[data-test="search-input"]')
    expect(elem.exists()).toBeTruthy()
  })
})
