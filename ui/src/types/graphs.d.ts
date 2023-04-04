import { TsResult, TimeRangeUnit } from '@/types/graphql'

type DataSets = TsResult[][]

interface MetricArgs {
  name: string
  monitor: string
  instance: string
  nodeId?: number
  timeRange: number
  timeRangeUnit: TimeRangeUnit
}

interface GraphProps {
  label: string
  metrics: string[]
  monitor: string
  nodeId?: number
  instance: string
  systemId?: string
  timeRange: number
  timeRangeUnit: TimeRangeUnit
}
