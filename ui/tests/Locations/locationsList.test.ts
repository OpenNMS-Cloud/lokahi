import mount  from '../mountWithPiniaVillus'
import LocationsList from '@/components/Locations/LocationsList.vue'

const mock = [
  {
    location: 'Default',
    status: 'WARNING',
    contextMenu: [
      { label: 'edit', handler: () => ({}) },
      { label: 'delete', handler: () => ({}) }
    ]
  }
]

let wrapper: any

describe('LocationsList', () => {
  beforeAll(() => {
    wrapper = mount({
      component: LocationsList,
      props: {
        items: mock
      },
      shallow: false
    })
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

  test('Should have a count', () => {
    const elem = wrapper.get('[data-test="count"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a help icon', () => {
    const elem = wrapper.get('[data-test="icon-help"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a header', () => {
    let elem = wrapper.get('[data-test="header"]')
    expect(elem.exists()).toBeTruthy()
  })

  test('Should have a list of location card', () => {
    const elem = wrapper.findAll('[data-test="card"]')
    expect(elem.length).toEqual(mock.length)
  })
})
