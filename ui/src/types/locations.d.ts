import { MonitoringLocation } from '@/types/graphql'

// temp
export interface LocationTemp extends MonitoringLocation {
  status?: string
}

export const enum DisplayType {
  ADD = 'add',
  EDIT = 'edit',
  LIST = 'list',
  READY = 'ready'
}
