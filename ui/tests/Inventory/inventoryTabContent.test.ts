import mount from '../mountWithPiniaVillus'
import InventoryTabContent from '@/components/Inventory/InventoryTabContent.vue'
import { InventoryNode, MonitoredStates, TimeUnit } from '@/types'

const tabContent: InventoryNode[] = [
  {
    id: 1,
    label: 'Monitored Node 1',
    status: '',
    metrics: [
      {
        type: 'latency',
        label: 'Latency',
        value: 9,
        timeUnit: TimeUnit.MSecs,
        status: 'UP'
      },
      {
        type: 'uptime',
        label: 'Uptime',
        value: 1667930274.66,
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
      tagValue: []
    },
    isNodeOverlayChecked: false,
    type: MonitoredStates.MONITORED
  }
]

let wrapper: any

describe('InventoryTabContent.vue', () => {
  beforeAll(() => {
    wrapper = mount({
      component: InventoryTabContent,
      props: {
        tabContent,
        state: MonitoredStates.MONITORED
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  const tabComponents = ['icon-storage', 'heading', 'metric-chip-list', 'text-anchor-list', 'icon-action-list']
  it.each(tabComponents)('should have "%s" components', (cmp) => {
    expect(wrapper.get(`[data-test="${cmp}"]`).exists()).toBe(true)
  })
})
