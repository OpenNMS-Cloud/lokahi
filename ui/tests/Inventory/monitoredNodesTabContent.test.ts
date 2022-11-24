import { mount } from '@vue/test-utils'
import MonitoredNodesTabContent from '@/components/Inventory/MonitoredNodesTabContent.vue'
import { NodeDetailContentType, TimeUnit } from '@/types'

const tab = {
  type: NodeDetailContentType.MONITORED,
  label: 'Monitored Nodes',
  nodes: [
    {
      id: 1,
      name: 'Monitored Node 1',
      metrics: [
        {
          type: 'latency',
          timestamp: 9,
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
      ],
      anchor: {
        profileValue: 75,
        profileLink: 'goto',
        locationValue: 'DefaultMinion',
        locationLink: 'goto',
        ipInterfaceValue: 25,
        ipInterfaceLink: 'goto',
        tagValue: 100,
        tagLink: 'goto'
      }
    }
  ]
}

let wrapper: any

describe('MonitoredNodesTabContent component', () => {
  beforeAll(() => {
    wrapper = mount(MonitoredNodesTabContent, {
      propsData: {
        tab
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  }) 

  it('should have required components', () => {
    const icon = wrapper.get('[data-test="icon"]')
    expect(icon.exists()).toBe(true)
    
    const heading = wrapper.get('[data-test="heading"]')
    expect(heading.exists()).toBe(true)

    const metricChipList = wrapper.get('[data-test="metric-chip-list"]')
    expect(metricChipList.exists()).toBe(true)

    const textAnchorList = wrapper.get('[data-test="text-anchor-list"]')
    expect(textAnchorList.exists()).toBe(true)

    const iconActionList = wrapper.get('[data-test="icon-action-list"]')
    expect(iconActionList.exists()).toBe(true)
  })
})