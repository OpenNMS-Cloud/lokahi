import { Minion } from './graphql'
import { Chip } from './metric'

interface ExtendedMinion extends Minion {
  latency?: Chip
  uptime?: Chip,
  status?: Chip
}
