import { flowsAppDataToChartJSDirection, flowsAppDataToChartJSTotal } from '@/dtos/chartJS.dto'
import { format } from 'date-fns'
import { defineStore } from 'pinia'
import { useflowsQueries } from '@/store/Queries/flowsQueries'
import { RequestCriteriaInput, TimeRange } from '@/types/graphql'
import { FlowsApplicationData, FlowsApplicationSummaries, ChartData } from '@/types'
import { IAutocompleteItemType } from '@featherds/autocomplete/src/components/types'

export const useFlowsStore = defineStore('flowsStore', {
  state: () => ({
    topApplications: [] as FlowsApplicationSummaries[],
    tableChartOptions: {},
    totalFlows: 0,
    filters: {
      dateFilter: TimeRange.Today,
      traffic: {
        selectedItem: 'total'
      },
      dataStyle: {
        selectedItem: 'table'
      },
      steps: 2000000,
      //Application AutoComplete
      applications: [] as IAutocompleteItemType[],
      selectedApplications: [],
      isApplicationsLoading: false,
      filteredApplications: [] as IAutocompleteItemType[],
      //Exporter AutoComplete
      exporters: [] as IAutocompleteItemType[],
      selectedExporters: [],
      isExportersLoading: false,
      filteredExporters: [] as IAutocompleteItemType[]
    },
    applications: {
      isTableLoading: false,
      isLineLoading: false,
      tableChartData: {} as ChartData,
      lineChartData: {} as ChartData,
      filterDialogOpen: false,
      //Table Data
      tableData: [{} as any],
      //Line Data
      lineTotalData: [{} as any],
      lineInboundData: [{} as any],
      lineOutboundData: [{} as any]
    },
    exporters: {
      isLoading: false,
      tableChartData: {} as ChartData,
      lineChartData: {} as ChartData,
      filterDialogOpen: false
    }
  }),
  actions: {
    async getDatasets() {
      const requestData = this.getRequestData()

      await this.getTableDataset(requestData)
      await this.getLineDataset(requestData)
    },
    async getApplications() {
      const flowsQueries = useflowsQueries()
      const requestData = this.getRequestData(50, undefined, [], [])

      const applications = (await flowsQueries.getApplications(requestData)) || []
      const applicationsAutocompleteObject = applications.value?.findApplications?.map((item: string) => ({
        _text: item.toUpperCase(),
        value: item
      })) as IAutocompleteItemType[]
      this.filters.applications = applicationsAutocompleteObject
    },
    async getExporters() {
      const flowsQueries = useflowsQueries()
      const requestData = this.getRequestData(undefined, undefined, [], [])

      const exporters = (await flowsQueries.getExporters(requestData)) || []
      const exportersAutocompleteObject = exporters.value?.findExporters?.map((item: any) => ({
        _text: item.node.nodeLabel.toUpperCase(),
        value: { nodeId: item.node.id as number, ipInterfaceId: item.ipInterface.id as number }
      })) as IAutocompleteItemType[]
      this.filters.exporters = exportersAutocompleteObject
    },
    async getTableDataset(requestData: RequestCriteriaInput) {
      const flowsQueries = useflowsQueries()
      this.applications.isTableLoading = true
      const applicationTableData = await flowsQueries.getApplicationsSummaries(requestData)
      if (applicationTableData.value?.findApplicationSummaries) {
        this.applications.tableData = [
          ...((applicationTableData.value?.findApplicationSummaries as FlowsApplicationSummaries[]) || [])
        ]
      }

      this.applications.isTableLoading = false
    },
    async getLineDataset(requestData: RequestCriteriaInput) {
      const flowsQueries = useflowsQueries()
      this.applications.isLineLoading = true
      const applicationsLineData = await flowsQueries.getApplicationsSeries(requestData)
      this.applications.isLineLoading = false

      if (applicationsLineData.value?.findApplicationSeries) {
        //Get Inbound Data
        this.applications.lineInboundData =
          flowsAppDataToChartJSDirection(
            applicationsLineData.value?.findApplicationSeries as FlowsApplicationData[],
            'INGRESS'
          ) || []

        //Get Outbound Data
        this.applications.lineOutboundData =
          flowsAppDataToChartJSDirection(
            applicationsLineData.value?.findApplicationSeries as FlowsApplicationData[],
            'EGRESS'
          ) || []

        //Get Total Data
        this.applications.lineTotalData =
          flowsAppDataToChartJSTotal(applicationsLineData.value?.findApplicationSeries as FlowsApplicationData[]) || []
        this.applications.isLineLoading = false
      }

      //Get Total Flows
      this.totalFlows = applicationsLineData.value?.findApplicationSeries?.length || 0
    },
    getRequestData(count = 10, step?: number, exporter?: object[], applications?: string[]) {
      return {
        count: count,
        step: step || this.filters.steps,
        exporter: exporter || this.filters.selectedExporters.map((exp: any) => exp.value),
        timeRange: this.getTimeRange(this.filters.dateFilter),
        applications: applications || this.filters.selectedApplications.map((app: any) => app.value)
      } as RequestCriteriaInput
    },
    createTableChartData() {
      if (this.applications.tableData) {
        this.applications.tableChartData = {
          labels: this.applications.tableData.map((row) => row.label),
          datasets: [
            {
              label: 'Inbound',
              data: this.applications.tableData.map((row) => row.bytesIn),
              barThickness: 13,
              backgroundColor: '#0043A4',
              hidden: this.filters.traffic.selectedItem === 'outbound'
            },
            {
              label: 'Outbound',
              data: this.applications.tableData.map((row) => row.bytesOut),
              barThickness: 13,
              backgroundColor: '#EE7D00',
              hidden: this.filters.traffic.selectedItem === 'inbound'
            }
          ]
        }
      }
    },
    createLineChartData() {
      if (this.applications.lineInboundData.length > 0) {
        const data = this.getLineChartDataForSelectedTraffic()
        const datasetArr = {
          type: 'line',
          datasets: data?.map((element, index) => {
            return {
              label: element.label,
              data: element.data.map((data: any) => {
                return {
                  x: this.convertToDate(data.timestamp),
                  y: data.value
                }
              }),
              fill: true,
              borderColor: this.randomColours(index),
              backgroundColor: this.randomColours(index, true)
            }
          })
        }
        this.applications.lineChartData = datasetArr
      }
    },
    async populateData() {
      await this.getExporters()
      await this.getApplications()
      await this.getDatasets()
      this.createCharts()
    },
    async updateChartData() {
      await this.getDatasets()
      this.createCharts()
    },
    createCharts() {
      this.createTableChartData()
      this.createLineChartData()
    },
    async trafficRadioOnChange(selectedItem: string) {
      this.filters.traffic.selectedItem = selectedItem
      this.createCharts()
    },
    convertToDate(ts: string) {
      const dateFormat = () => {
        switch (this.filters.dateFilter) {
          case TimeRange.Today:
            return 'HH:mm'
          case TimeRange.Last_24Hours:
            return 'HH:mm'
          case TimeRange.SevenDays:
            return 'dd/MMM HH:mm'
          default:
            return 'dd/MMM HH:mm'
        }
      }
      return format(new Date(ts), dateFormat())
    },
    getLineChartDataForSelectedTraffic() {
      switch (this.filters.traffic.selectedItem) {
        case 'inbound':
          return this.applications.lineInboundData
        case 'outbound':
          return this.applications.lineOutboundData
        case 'total':
          return this.applications.lineTotalData
        default:
          return this.applications.lineTotalData
      }
    },
    async onDateFilterUpdate(e: any) {
      this.filters.dateFilter = e
      await this.updateChartData()
    },
    getTimeRange(range: string) {
      const now = new Date()
      let startTime
      switch (range) {
        case TimeRange.Today:
          startTime = new Date(new Date().setHours(0, 0, 0, 0)).getTime()
          return { startTime: startTime, endTime: Date.now() }
        case TimeRange.Last_24Hours:
          startTime = now.setDate(now.getDate() - 1)
          return { startTime: startTime, endTime: Date.now() }
        case TimeRange.SevenDays:
          startTime = now.setDate(now.getDate() - 7)
          return { startTime: startTime, endTime: Date.now() }
        default:
          startTime = new Date(new Date().setHours(0, 0, 0, 0)).getTime()
          return { startTime: startTime, endTime: Date.now() }
      }
    },
    applicationsAutoCompleteSearch(searchString: string) {
      let timeout = -1
      this.filters.isApplicationsLoading = true
      clearTimeout(timeout)
      timeout = window.setTimeout(() => {
        this.filters.filteredApplications = this.filters.applications
          .filter((x: any) => x._text.toLowerCase().indexOf(searchString) > -1)
          .map((x: any) => ({
            value: x.value,
            _text: x._text
          }))
        this.filters.isApplicationsLoading = false
      }, 500)
    },
    exportersAutoCompleteSearch(searchString: string) {
      let timeout = -1
      this.filters.isExportersLoading = true
      clearTimeout(timeout)
      timeout = window.setTimeout(() => {
        this.filters.filteredExporters = this.filters.exporters
          .filter((x: any) => x._text.toLowerCase().indexOf(searchString) > -1)
          .map((x: any) => ({
            value: x.value,
            _text: x._text
          }))
        this.filters.isExportersLoading = false
      }, 500)
    },
    // This method is needed as currently on update of chart data, new data values are not being assigned a colour.
    randomColours(index: number, opacity = false) {
      const defaultColors = [
        '#3366CC',
        '#DC3912',
        '#FF9900',
        '#109618',
        '#990099',
        '#3B3EAC',
        '#0099C6',
        '#DD4477',
        '#66AA00',
        '#B82E2E',
        '#316395',
        '#994499',
        '#22AA99',
        '#AAAA11',
        '#6633CC',
        '#E67300',
        '#8B0707',
        '#329262',
        '#5574A6',
        '#651067'
      ]
      const addOpacity = function (hex: string, opacity: number) {
        const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
        return result
          ? 'rgba(' +
              parseInt(result[1], 16) +
              ', ' +
              parseInt(result[2], 16) +
              ', ' +
              parseInt(result[3], 16) +
              ', ' +
              opacity +
              ')'
          : hex
      }
      if (opacity) {
        return addOpacity(defaultColors[index], 0.3)
      } else {
        return defaultColors[index]
      }
    },
    async getApplicationDataset() {
      const flowsQueries = useflowsQueries()
      const requestData = this.getRequestData(10, 2000000, [], [])

      const topApplications = await flowsQueries.getApplicationsSummaries(requestData)
      this.topApplications = [
        ...((topApplications.value?.findApplicationSummaries as FlowsApplicationSummaries[]) || [])
      ]
    }
  }
})
