import { flowsAppDataToChartJSDirection, flowsAppDataToChartJSTotal } from '@/dtos/chartJS.dto'
import { defineStore } from 'pinia'
import { useflowsQueries } from '@/store/Queries/flowsQueries'
import { RequestCriteriaInput, TimeRange } from '@/types/graphql'
import { FlowsApplicationData, FlowsApplicationSummaries, ChartData, IExporter, FlowsLineChartItem } from '@/types'
import { useFlowsStore } from './flowsStore'
import { get } from 'lodash'

export const useFlowsApplicationStore = defineStore('flowsApplicationStore', {
  state: () => ({
    topApplications: [] as FlowsApplicationSummaries[],
    totalFlows: 0,
    isTableLoading: false,
    isLineLoading: false,
    tableChartData: {} as ChartData,
    lineChartData: {} as ChartData,
    hasLineData: false,
    hasTableData: false,
    tableData: [{} as any],
    lineTotalData: [] as FlowsLineChartItem[],
    lineInboundData: [] as FlowsLineChartItem[],
    lineOutboundData: [] as FlowsLineChartItem[]
  }),
  actions: {
    async getApplicationDatasets() {
      const flowsStore = useFlowsStore()
      const requestData = flowsStore.getRequestData()
      await this.getApplicationTableDataset(requestData)
      await this.getApplicationLineDataset(requestData)
    },
    async getApplicationTableDataset(requestData: RequestCriteriaInput) {
      this.tableData = []
      const flowsQueries = useflowsQueries()
      this.isTableLoading = true
      const applicationTableData = await flowsQueries.getApplicationsSummaries(requestData)
      if (applicationTableData.value?.findApplicationSummaries) {
        this.tableData = [
          ...((applicationTableData.value?.findApplicationSummaries as FlowsApplicationSummaries[]) || [])
        ]
      }
    },
    async getApplicationLineDataset(requestData: RequestCriteriaInput) {
      const flowsQueries = useflowsQueries()
      this.isLineLoading = true
      const applicationsLineData = await flowsQueries.getApplicationsSeries(requestData)

      if (applicationsLineData.value?.findApplicationSeries) {
        //Get Inbound Data
        this.lineInboundData =
          flowsAppDataToChartJSDirection(
            applicationsLineData.value?.findApplicationSeries as FlowsApplicationData[],
            'INGRESS'
          ) || []

        //Get Outbound Data
        this.lineOutboundData =
          flowsAppDataToChartJSDirection(
            applicationsLineData.value?.findApplicationSeries as FlowsApplicationData[],
            'EGRESS'
          ) || []

        //Get Total Data
        this.lineTotalData =
          flowsAppDataToChartJSTotal(applicationsLineData.value?.findApplicationSeries as FlowsApplicationData[]) || []
      }

      //Get Total App Flows
      this.totalFlows = applicationsLineData.value?.findApplicationSeries?.length || 0
    },
    createApplicationTableChartData() {
      const flowsStore = useFlowsStore()

      if (this.tableData.length > 0) {
        this.tableChartData = {
          labels: this.tableData.map((row) => row.label),
          datasets: [
            {
              label: 'Inbound',
              data: this.tableData.map((row) => row.bytesIn),
              barThickness: 13,
              backgroundColor: '#0043A4',
              hidden: flowsStore.filters.traffic.selectedItem === 'outbound'
            },
            {
              label: 'Outbound',
              data: this.tableData.map((row) => row.bytesOut),
              barThickness: 13,
              backgroundColor: '#EE7D00',
              hidden: flowsStore.filters.traffic.selectedItem === 'inbound'
            }
          ]
        }
        this.hasTableData = true
      } else {
        this.hasTableData = false
      }
      this.isTableLoading = false
    },
    createApplicationLineChartData() {
      const flowsStore = useFlowsStore()

      if (this.lineTotalData.length > 0) {
        const data = this.getLineChartDataForSelectedTraffic()
        const datasetArr = {
          type: 'line',
          datasets: data?.map((element: any, index: number) => {
            return {
              label: element.label,
              data: element.data.map((data: any) => {
                return {
                  x: flowsStore.convertToDate(data.timestamp),
                  y: data.value
                }
              }),
              fill: true,
              borderColor: flowsStore.randomColours(index),
              backgroundColor: flowsStore.randomColours(index, true)
            }
          })
        }
        this.lineChartData = datasetArr
        this.hasLineData = true
      } else {
        this.hasLineData = false
      }
      this.isLineLoading = false
    },
    getLineChartDataForSelectedTraffic() {
      const flowsStore = useFlowsStore()
      switch (flowsStore.filters.traffic.selectedItem) {
        case 'inbound':
          return this.lineInboundData
        case 'outbound':
          return this.lineOutboundData
        case 'total':
          return this.lineTotalData
        default:
          return this.lineTotalData
      }
    },
    async getApplicationDataset() {
      const flowsQueries = useflowsQueries()
      const flowsStore = useFlowsStore()
      flowsStore.filters.dateFilter = TimeRange.Last_24Hours
      const exporter = get(flowsStore.filters.selectedExporterTopApplication, 'value') as IExporter
      const exporters: IExporter[] = exporter ? [exporter] : []
      const requestData = flowsStore.getRequestData(10, 2000000, exporters, [])

      const topApplications = await flowsQueries.getApplicationsSummaries(requestData)
      flowsStore.topApplications = [
        ...((topApplications.value?.findApplicationSummaries as FlowsApplicationSummaries[]) || [])
      ]
    }
  }
})
