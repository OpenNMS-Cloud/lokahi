import { shallowMount } from '@vue/test-utils'
import LocationsEditForm from '@/components/Locations/LocationsEditForm.vue'

let wrapper: any

describe('LocationsEditForm', () => {
  beforeAll(() => {
    wrapper = shallowMount(LocationsEditForm)
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

  test('Should have a regenrate key button', () => {
    const elem = wrapper.get('[data-test="regenrate-key-button"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a download button', () => {
    const elem = wrapper.get('[data-test="download-button"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have an intructions section', () => {
    const elem = wrapper.get('[data-test="instruction-section"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a save button', () => {
    const elem = wrapper.get('[data-test="save-button"]')
    expect(elem.exists()).toBeTruthy()
  })
})
