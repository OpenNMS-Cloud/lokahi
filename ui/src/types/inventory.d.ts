import { Tag } from './graphql'
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
  type: MonitoredStates.MONITORED
}

interface UnmonitoredNode {
  id: number
  label: string
  anchor: Anchor
  isNodeOverlayChecked: boolean
  type: MonitoredStates.UNMONITORED
}

interface DetectedNode {
  id: number
  label: string
  anchor: Anchor
  isNodeOverlayChecked: boolean
  type: MonitoredStates.DETECTED
}

export const enum MonitoredStates {
  MONITORED = 'MONITORED',
  UNMONITORED = 'UNMONITORED',
  DETECTED = 'DETECTED'
}

type InventoryNode = MonitoredNode | UnmonitoredNode | DetectedNode
