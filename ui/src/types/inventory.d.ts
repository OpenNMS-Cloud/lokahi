import { MonitoredState, Tag } from './graphql'
import { Chip } from './metric'

export interface Anchor {
  profileValue?: number | string
  profileLink?: string
  locationValue?: string
  locationLink?: string
  managementIpValue?: string
  managementIpLink?: string
  tagValue: Tag[]
}

interface MonitoredNode {
  id: number
  label: string | undefined
  status: string
  metrics: Chip[]
  anchor: Anchor
  isNodeOverlayChecked: boolean
  type: MonitoredState.Monitored
}

interface UnmonitoredNode {
  id: number
  label: string
  anchor: Anchor
  isNodeOverlayChecked: boolean
  type: MonitoredState.Unmonitored
}

interface DetectedNode {
  id: number
  label: string
  anchor: Anchor
  isNodeOverlayChecked: boolean
  type: MonitoredState.Detected
}

type InventoryNode = MonitoredNode | UnmonitoredNode | DetectedNode
