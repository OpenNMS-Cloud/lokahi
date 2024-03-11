import { Alert } from './graphql'
import { TimeRange } from '@/types/graphql'

interface IAlert extends Alert {
  isSelected?: boolean
  label?: string
  nodeType?: string
}

interface Pagination {
  page: number
  pageSize: number
  total: number
}

interface AlertsFilters {
  timeRange: TimeRange
  search?: string
  severities?: string[]
  sortAscending: boolean
  sortBy?: string
  nodeLabel?: string,
  nodeId?: number
}

interface Variables {
  id?: number;
}
