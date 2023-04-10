import { Tag } from './graphql'
import { Chip } from './metric'

interface Anchor {
  profileValue?: number | string
  profileLink?: string
  locationValue?: string
  locationLink?: string
  managementIpValue?: string
  managementIpLink?: string
  tagValue: Tag[]
}

interface NodeContent {
  id: number
  label: string | undefined
  status: string
  metrics: Chip[]
  anchor: Anchor
  isNodeOverlayChecked: boolean
}
