import { BGColors } from '@/components/Appliances/utils'
import { Minion } from './graphql'

interface ExtendedMinion extends Minion {
  icmp_latency?: number | undefined
  snmp_uptime?: number | undefined
  status?: string
}

interface ExtendedMinionWithBGColors extends ExtendedMinion, BGColors {}