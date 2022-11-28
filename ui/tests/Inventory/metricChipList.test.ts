import { mount } from '@vue/test-utils'
import { TimeUnit } from '@/types'
import { Chip } from '@/types/metric'
import MetricChipList from '@/components/Inventory/MetricChipList.vue'

const items: Chip[] = [
  {
    type: 'latency',
    timestamp: -1667930274660,
    timeUnit: TimeUnit.MSecs,
    status: 'UP'
  },
  {
    type: 'uptime',
    timestamp: 1667930274.660,
    timeUnit: TimeUnit.Secs,
    status: 'DOWN'
  },
  {
    type: 'status',
    status: 'DOWN'
  }
]

let wrapper: any

describe('Metric chip list', () => {
  beforeAll(() => {
    wrapper = mount(MetricChipList, { 
      propsData: {
        items
      }
    })
  })
  afterAll(() => {
    wrapper.unmount() 
  })

  it(`should have ${items.length} metric chip(s)`, () => {
    const chipItem = wrapper.findAllComponents('[data-test="metric-chip"]')
    expect(chipItem.length).toEqual(items.length)   
  })
})