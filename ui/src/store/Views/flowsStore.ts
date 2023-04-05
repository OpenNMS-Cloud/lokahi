import { flowsAppDataToChartJS } from '@/dtos/chartJS.dto'
import { ChartData } from 'chart.js'
import { format } from 'date-fns'
import { defineStore } from 'pinia'
import { useflowsQueries } from '@/store/Queries/flowsQueries'
import { RequestCriteriaInput, TimeRange } from '@/types/graphql'
import { FlowsApplicationData, FlowsApplicationSummaries } from '@/types'

const flowsQueries = useflowsQueries()

export const useFlowsStore = defineStore('flowsStore', {
  state: () => ({
    tableDatasets: [{} as any],
    lineDatasets: [{} as any],
    tableChartOptions: {},
    totalFlows: '1,957',
    filters: {
      dateFilter: TimeRange.Today,
      traffic: {
        selectedItem: 'total'
      },
      dataStyle: {
        selectedItem: 'table'
      },
      applications: ['']
    },
    applications: {
      isTableLoading: false,
      isLineLoading: false,
      tableChartData: {} as ChartData,
      lineChartData: {} as ChartData,
      expansionOpen: true,
      filterDialogOpen: false,
      dialogFilters: { ...defaultDialogFilters }
    },
    exporters: {
      isLoading: false,
      tableChartData: {} as ChartData,
      lineChartData: {} as ChartData,
      expansionOpen: true,
      filterDialogOpen: false,
      dialogFilters: { ...defaultDialogFilters }
    }
  }),
  actions: {
    async getDatasets() {
      //Will be replaced with a BE call
      const requestData = {
        count: 10,
        step: 2000000,
        timeRange: this.getTimeRange(this.filters.dateFilter)
      } as RequestCriteriaInput

      this.applications.isTableLoading = true
      //Get Table Data
      const applicationTableData = await flowsQueries.getApplicationsSummaries(requestData)
      this.tableDatasets = [
        ...((applicationTableData.value?.findApplicationSummaries as FlowsApplicationSummaries[]) || null)
      ]
      this.applications.isTableLoading = false

      this.applications.isLineLoading = true
      //Get Line Graph Data
      const applicationsLineData = await flowsQueries.getApplicationsSeries(requestData)
      this.lineDatasets = [
        ...(flowsAppDataToChartJS(applicationsLineData.value?.findApplicationSeries as FlowsApplicationData[]) || null)
      ]
      this.applications.isLineLoading = false
    },
    createTableChartData() {
      if (this.tableDatasets) {
        this.exporters.tableChartData = {
          labels: this.tableDatasets.map((row) => row.label),
          datasets: [
            {
              label: 'Inbound',
              data: this.tableDatasets.map((row) => row.bytesIn),
              barThickness: 13,
              backgroundColor: '#0043A4',
              hidden: this.filters.traffic.selectedItem === 'outbound'
            },
            {
              label: 'Outbound',
              data: this.tableDatasets.map((row) => row.bytesOut),
              barThickness: 13,
              backgroundColor: '#EE7D00',
              hidden: this.filters.traffic.selectedItem === 'inbound'
            }
          ]
        }
      }
      if (this.tableDatasets) {
        this.applications.tableChartData = {
          labels: this.tableDatasets.map((row) => row.label),
          datasets: [
            {
              label: 'Inbound',
              data: this.tableDatasets.map((row) => row.bytesIn),
              barThickness: 13,
              backgroundColor: '#0043A4',
              hidden: this.filters.traffic.selectedItem === 'outbound'
            },
            {
              label: 'Outbound',
              data: this.tableDatasets.map((row) => row.bytesOut),
              barThickness: 13,
              backgroundColor: '#EE7D00',
              hidden: this.filters.traffic.selectedItem === 'inbound'
            }
          ]
        }
      }
    },
    createLineChartData() {
      if (this.lineDatasets) {
        const datasetArr = {
          type: 'line',
          datasets: this.lineDatasets.map((element) => {
            return {
              label: element.label,
              data: element.data.map((data: any) => {
                return {
                  x: this.convertToDate(data.timestamp),
                  y: data.value
                }
              }),
              fill: true
            }
          })
        }
        this.applications.lineChartData = datasetArr
        this.exporters.lineChartData = datasetArr
      }
    },
    filterDialogToggle(event: Event, isAppFilter: boolean) {
      isAppFilter
        ? (this.applications.filterDialogOpen = !this.applications.filterDialogOpen)
        : (this.exporters.filterDialogOpen = !this.exporters.filterDialogOpen)
    },
    async updateCharts() {
      await this.getDatasets()
      this.createTableChartData()
      this.createLineChartData()
    },
    createCharts() {
      this.createTableChartData()
      this.createLineChartData()
    },
    appDialogRefreshClick(e: Event) {
      const selectedFilters = this.getTrueValuesFromObject(this.applications.dialogFilters)
      console.log('you have selected ' + selectedFilters)
      this.filterDialogToggle(e, true)
    },
    expDialogRefreshClick(e: Event) {
      const selectedFilters = this.getTrueValuesFromObject(this.exporters.dialogFilters)
      console.log('you have selected ' + selectedFilters)
      this.filterDialogToggle(e, false)
    },
    getTrueValuesFromObject(object: object) {
      const keys = Object.keys(object)
      const filtered = keys.filter(function (key: string) {
        return object[key as keyof typeof object]
      })
      return filtered
    },
    async trafficRadioOnChange(selectedItem: string) {
      this.filters.traffic.selectedItem = selectedItem
      await this.updateCharts()
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
    async onDateFilterUpdate(e: any) {
      this.filters.dateFilter = e
      await this.updateCharts()
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
    }
  }
})

type FlowsDialogFilters = {
  http: boolean
  https: boolean
  pandoPub: boolean
  snmp: boolean
  imaps: boolean
}

const defaultDialogFilters: FlowsDialogFilters = {
  http: false,
  https: false,
  pandoPub: false,
  snmp: false,
  imaps: false
}
