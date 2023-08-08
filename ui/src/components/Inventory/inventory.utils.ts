import { InventoryNode, MonitoredStates } from '@/types'

export const isMonitored = (node: InventoryNode): node is InventoryNode => {
  return (node as InventoryNode).type === MonitoredStates.MONITORED
}
