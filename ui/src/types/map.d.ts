import { SORT } from '@featherds/table'

export interface Category {
  authorizedGroups: string[]
  id: number
  name: string
}

export interface QueryParameters {
  limit?: number
  offset?: number
  _s?: string
  orderBy?: string
  order?: SORT
  search?: string
  groupBy?: string
  groupByValue?: string
  [x: string]: any
}

export interface AlarmQueryParameters {
  ack?: boolean
  clear?: boolean
  escalate?: boolean
}

export interface AlarmModificationQueryVariable {
  pathVariable: string
  queryParameters: AlarmQueryParameters
}
