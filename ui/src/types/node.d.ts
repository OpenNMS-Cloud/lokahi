import { BGColors } from '@/components/Appliances/utils'
import { Node } from './graphql'

interface ExtendedNode extends Node {
  icmp_latency: number
  snmp_uptime: number
  status: string
}

interface ExtendedNodeWithBGColors extends ExtendedNode, BGColors {}
