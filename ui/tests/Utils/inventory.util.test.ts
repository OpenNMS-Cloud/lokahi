import { isMonitored } from '@/components/Inventory/inventory.utils'
import { InventoryNode, MonitoredStates } from '@/types'

describe('Inventory Utility Test', () => {
  test('The inventory utility can tell if a node is monitored', () => {
    const amIMonitored = isMonitored({type:MonitoredStates.MONITORED} as InventoryNode)
    expect(amIMonitored).toEqual(true)
  })
  test('The inventory utility can tell if a node is not monitored', () => {
    const amIMonitored = isMonitored({type:MonitoredStates.UNMONITORED} as InventoryNode)
    expect(amIMonitored).toEqual(false)
  })

})
