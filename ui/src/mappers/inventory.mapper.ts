import { InventoryItem, InventoryPage, NewInventoryNode, RawMetrics } from '@/types'

export const fromServer = (allNodes: Array<NewInventoryNode>, allMetrics: RawMetrics): InventoryPage => {
  const nodesWithMetrics: Array<InventoryItem> = []

  for (const [i, device] of allNodes.entries()) {
    const res = allMetrics.data.result.find((d) => Number(d.metric.node_id) === device.id)
    nodesWithMetrics[i] = device
    nodesWithMetrics[i].metrics = res
  }
  return {
    nodes: nodesWithMetrics
  }
}
