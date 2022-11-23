import { mount } from '@vue/test-utils'
import { FeatherTab } from '@featherds/tabs'
import TabList from '@/components/Inventory/TabList.vue'
import DetectedNodesTabContent from '@/components/Inventory/DetectedNodesTabContent.vue'
import MonitoredNodesTabContent from '@/components/Inventory/MonitoredNodesTabContent.vue'
import { NodeDetailContentType } from '@/types'
import { TabNode } from '@/types/inventory'

const tabs: TabNode[] = [
  {
    type:  NodeDetailContentType.MONITORED,
    label: 'Monitored Nodes',
    nodes: [
      {
        header: 'Node 1'
      },
      {
        header: 'Node 11'
      }
    ]
  },
  {
    type:  NodeDetailContentType.MONITORED,
    label: 'Unmonitored Nodes',
    nodes: [
      {
        header: 'Node 2'
      },
      {
        header: 'Node 22'
      }
    ]
  },
  {
    type:  NodeDetailContentType.DETECTED,
    label: 'Detected Nodes',
    nodes: [
      {
        header: 'Node 3'
      },
      {
        header: 'Node 33'
      }
    ]
  }
]

let wrapper: any

describe('Inventory node tab list', () => {
  beforeAll(() => {
    wrapper = mount(TabList, {
      props: {
        tabs
      }
    })
  })
  afterAll(() => {
    wrapper.unmount() 
  })

  it(`should have ${tabs.length} tabs`, () => {
    const featherTabs = wrapper.findAllComponents(FeatherTab)
    expect(featherTabs.length).toEqual(tabs.length)
  })

  it('should have the required tab content components', () => {
    const expectedDetectedComponents = tabs.filter(({type}) => type === NodeDetailContentType.DETECTED).length
    const detectedComponents = wrapper.findAllComponents(DetectedNodesTabContent).length
    expect(expectedDetectedComponents).toEqual(detectedComponents)

    const expectedMonitoredComponents = tabs.filter(({type}) => type === NodeDetailContentType.MONITORED).length
    const monitoredComponents = wrapper.findAllComponents(MonitoredNodesTabContent).length
    expect(expectedMonitoredComponents).toEqual(monitoredComponents)
  })
})