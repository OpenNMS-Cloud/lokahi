import { shallowMount } from '@vue/test-utils'
import LocationsMinionsCard from '@/components/Locations/LocationsMinionsCard.vue'

const mock = [
  {
    id: 1,
    name: 'minion0',
    version: 'v.0.0.0',
    latency: '000m',
    status: 'UP',
    utillization: '00%',
    ip: '000.000.000.000',
    contextMenu: [
      { label: 'edit', handler: () => ({}) },
      { label: 'delete', handler: () => ({}) }
    ]
  }
]

let wrapper: any

describe.skip('LocationsMinionsCard', () => {
  beforeAll(() => {
    wrapper = shallowMount(LocationsMinionsCard, { propsData: { item: mock } })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(expect).toBeTruthy()
  })

  const headers = ['name', 'latency', 'status', 'utilization', 'ip']
  test.each(headers)('Should have `%s` header', (header) => {
    const elem = wrapper.get(`[data-test="header-${header}"]`)
    expect(elem.exists).toBeTruthy()
  })

  const content = ['version', 'latency', 'status', 'utilization', 'ip']
  test.each(content)('Should have `%s` content', (content) => {
    const elem = wrapper.get(`[data-test="content-${content}"]`)
    expect(elem.exists).toBeTruthy()
  })

  test('should have a context menu', () => {
    const elem = wrapper.get('[data-test="context-menu"]')
    expect(elem.exists).toBeTruthy()
  })
})
