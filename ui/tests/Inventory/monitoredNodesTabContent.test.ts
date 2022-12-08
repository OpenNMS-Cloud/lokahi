import { mount } from '@vue/test-utils'
import MonitoredNodesTabContent from '@/components/Inventory/MonitoredNodesTabContent.vue'
import { TimeUnit } from '@/types'

const tabContent = [
  {
    id: 1,
    label: 'Monitored Node 1',
    status: '',
    metrics: [
      {
        type: 'latency',
        label: 'Latency',
        timestamp: 9,
        timeUnit: TimeUnit.MSecs,
        status: 'UP'
      },
      {
        type: 'uptime',
        label: 'Uptime',
        timestamp: 1667930274.660,
        timeUnit: TimeUnit.Secs,
        status: 'DOWN'
      },
      {
        type: 'status',
        label: 'Status',
        status: 'DOWN'
      }
    ],
    anchor: {
      profileValue: 75,
      profileLink: 'goto',
      locationValue: 'DefaultMinion',
      locationLink: 'goto',
      managementIpValue: '0.0.0.0',
      managementIpLink: 'goto',
      tagValue: 100,
      tagLink: 'goto'
    }
  }
]

let wrapper: any

describe('MonitoredNodesTabContent component', () => {
  beforeAll(() => {
    wrapper = mount(MonitoredNodesTabContent, {
      shallow: true,
      props: {
        tabContent
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  }) 

  const tabComponents = [
    'icon',
    'heading',
    'metric-chip-list',
    'text-anchor-list',
    'icon-action-list'
  ]
  it.each(tabComponents)('should have "%s" components', (cmp) => {
    expect(wrapper.get(`[data-test="${cmp}"]`).exists()).toBe(true)
  })
})