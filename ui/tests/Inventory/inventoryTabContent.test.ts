import mount from '../mountWithPiniaVillus'
import InventoryTabContent from '@/components/Inventory/InventoryTabContent.vue'
import { InventoryItem, InventoryNode, MonitoredStates, TimeUnit } from '@/types'

const tabContent: InventoryItem[] = [
  {
    id: 1,
    nodeLabel: 'Monitored Node 1',
    
    location: {id:-1, location:''},
    metrics: {
      metric:{instance: '192.168.1.1',__name__:'response_time_msec',location_id:'1',monitor:'ICMP',node_id:'1',system_id:'default'},
      value: [79824378,12.22],
      values: []
    },
    tags: [{
      id:1, name: 'default'
    }],
    monitoredState: 'MONITORED',
    ipInterfaces: [{id:1,ipAddress:'192.168.1.1',nodeId:1,snmpPrimary:true}],
    monitoringLocationId:1,
    scanType:'DISCOVERY_SCAN'
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
    if (wrapper && wrapper.unmount){
      wrapper.unmount()
    }
  })

  const tabComponents = ['heading', 'text-anchor-list']
  it.each(tabComponents)('should have "%s" components', (cmp) => {
    expect(wrapper.get(`[data-test="${cmp}"]`).exists()).toBe(true)
  })
})
