import { InventoryNode, MonitoredNode } from '@/types'
import { MonitoredState } from '@/types/graphql'

export const isMonitored = (node: InventoryNode): node is MonitoredNode => {
  return node.type === MonitoredState.Monitored
}
