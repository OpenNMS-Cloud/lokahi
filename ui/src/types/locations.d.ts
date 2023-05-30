import { MonitoringLocation } from '@/types/graphql'
import { ContextMenuItem } from '@/types'

// temp
export interface LocationTemp extends MonitoringLocation {
  status?: string
  contextMenu?: ContextMenuItem[]
}

export const enum DisplayType {
  ADD = 'add',
  EDIT = 'edit',
  LIST = 'list'
}
