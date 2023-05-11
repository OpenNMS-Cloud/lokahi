import { shallowMount } from '@vue/test-utils'
import LocationsAddForm from '@/components/Locations/LocationsAddForm.vue'

let wrapper: any

describe('LocationsAddForm', () => {
  beforeAll(() => {
    wrapper = shallowMount(LocationsAddForm)
  })
  afterAll(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('Should have a headline', () => {
    const elem = wrapper.get('[data-test="headline"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have an input name', () => {
    const elem = wrapper.get('[data-test="input-name"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have an input longitude', () => {
    const elem = wrapper.get('[data-test="input-longitude"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have an input latitude', () => {
    const elem = wrapper.get('[data-test="input-latitude"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a save button', () => {
    const elem = wrapper.get('[data-test="save-button"]')
    expect(elem.exists()).toBeTruthy()
  })
})
