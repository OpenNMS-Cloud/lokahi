import { DetectedNode, MonitoredStates, MonitoredNode, UnmonitoredNode } from '@/types'

export const isMonitored = (node: MonitoredNode | UnmonitoredNode | DetectedNode): node is MonitoredNode => {
  return (node as MonitoredNode).type === MonitoredStates.MONITORED
}
