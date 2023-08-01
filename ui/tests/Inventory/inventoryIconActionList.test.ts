import mount from '../mountWithPiniaVillus'
import InventoryIconActionList from '@/components/Inventory/InventoryIconActionList.vue'
import { MonitoredStates } from '@/types'

let wrapper: any

const mockMonitoredNode = {
  id: 1,
  label: 'node',
  status: 'UP',
  metrics: [],
  anchor: {},
  isNodeOverlayChecked: false,
  type: MonitoredStates.MONITORED
}

describe('InventoryIconActionList.vue', () => {
  beforeAll(() => {
    wrapper = mount({
      component: InventoryIconActionList,
      shallow: false,
      props: {
        node: mockMonitoredNode
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  const actionList = [
    'line-chart',
    'warning',
    'delete'
  ]
  it.each(actionList)('should have "%s" action icon', (icon) => {
    expect(wrapper.get(`[data-test="${icon}"]`).exists()).toBe(true)
  })
})
