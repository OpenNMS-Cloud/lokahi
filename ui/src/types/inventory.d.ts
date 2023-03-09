import { Chip } from './metric'

interface Anchor {
  profileValue?: number | string
  profileLink?: string
  locationValue?: string
  locationLink?: string
  managementIpValue?: string
  managementIpLink?: string
  tagValue?: array
}

interface NodeContent {
  id: number
  label: string | undefined
  status: string
  metrics: Chip[]
  anchor: Anchor
  isNodeOverlayChecked: boolean
}
