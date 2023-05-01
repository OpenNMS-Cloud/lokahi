export const enum ChartTypes {
  BAR = 'bar',
  LINE = 'line',
  SCATTER = 'scatter',
  PIE = 'pie',
  DOUGHNUT = 'doughnut'
}

export interface FlowsLineChartItem {
  label: string
  data: FlowsApplicationChartData[]
}

export interface FlowsApplicationData {
  timestamp: string
  value: number
  direction: string
  label: string
}
export interface FlowsApplicationChartData {
  timestamp: string
  value: number
  direction: string
}

export interface FlowsApplicationSummaries {
  label: string
  bytesIn: number
  bytesOut: number
}

export interface IExporter {
  nodeId?: number
  ipInterfaceId?: number
}
