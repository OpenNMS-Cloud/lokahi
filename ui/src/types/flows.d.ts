export const enum ChartTypes {
  BAR = 'bar',
  LINE = 'line',
  SCATTER = 'scatter',
  PIE = 'pie',
  DOUGHNUT = 'doughnut'
}

export interface FlowsLineChartItem {
  label: string
  data: FlowsLineChartItemData[]
}

export interface FlowsLineChartItemData {
  timestamp: string
  value: number
  direction: string
}
