import { Node } from './graphql'
import { Chip } from './metric'

interface ExtendedNode extends Node {
  latency?: Chip
  status?: string | undefined
}
