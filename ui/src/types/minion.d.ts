import { BGColors } from '@/components/Appliances/appliances.helpers'
import { MinionDto } from './graphql'

interface ExtendedMinionDTO extends MinionDto {
  icmp_latency: number
  snmp_uptime: number
}

interface ExtendedMinionDTOWithBGColors extends ExtendedMinionDTO, BGColors {}