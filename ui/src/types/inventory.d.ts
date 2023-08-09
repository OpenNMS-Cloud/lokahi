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

interface InventoryNode {
  id: number
  label: string | undefined
  status: string
  metrics: Chip[]
  anchor: Anchor
  isNodeOverlayChecked: boolean
  type: MonitoredStates.DETECTED | MonitoredStates.MONITORED | MonitoredStates.UNMONITORED
}

interface NewInventoryNode {
  id: number
  ipInterfaces: [{id:number,ipAddress:string,nodeId:number,snmpPrimary:boolean}],
  location:{id:number,location:string},
  monitoredState: string,
  monitoringLocationId: number,
  nodeLabel: string,
  scanType: string,
  tags: [{id:number, name: string}],
}
interface RawMetric {
  metric: {
    __name__:string,
    instance: string, 
    location_id: string,
    monitor:string,
    node_id:string,
    system_id:string
  },
  value: [number,number],
  values: []
}

interface RawMetrics {
  status:string,
  data:{
    resultType:string,
    result:Array<RawMetric>
  }
}


export interface InventoryItem extends NewInventoryNode{
    metrics?: RawMetric
}
export interface InventoryPage {
    nodes: Array<InventoryItem>;
}


export const enum MonitoredStates {
  MONITORED = 'MONITORED',
  UNMONITORED = 'UNMONITORED',
  DETECTED = 'DETECTED'
}

