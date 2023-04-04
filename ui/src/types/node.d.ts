import { SnmpInterface, Location, IpInterface, DefaultNode } from './graphql'
import { Chip } from './metric'

interface ExtendedNode extends DefaultNode {
  latency?: Chip
  status?: string | undefined
}
