import { Chip } from './metric'

// TODO: cause error when importing
/* export enum NodeDetailContentType {
  MONITORED,
  DETECTED
} */

export interface Anchor {
  profileValue: number,
  profileLink: string,
  locationValue: string,
  locationLink: string,
  ipInterfaceValue: number,
  ipInterfaceLink: string,
  tagValue: number,
  tagLink: string,
}

interface NodeDetail {
  id: number,
  name: string,
  metrics: Chip[],
  anchor: Anchor
}

export interface TabNode {
  type: number,
  label: string,
  nodes: NodeDetail[]
}
