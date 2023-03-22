import { ChartData } from 'chart.js'
import { defineStore } from 'pinia'

export const useFlowsStore = defineStore('flowsStore', {
  state: () => ({
    datasets: [{} as any],
    tableChartOptions: {},
    totalFlows: '1,957',
    filters: {
      dateFilter: '2023-1-14 to 2023-1-15',
      traffic: {
        selectedItem: 'total'
      },
      dataStyle: {
        selectedItem: 'table'
      }
    },
    applications: {
      tableChartData: {} as ChartData,
      expansionOpen: true,
      filterDialogOpen: false,
      dialogFilters: { ...defaultDialogFilters }
    },
    exporters: {
      tableChartData: {} as ChartData,
      expansionOpen: true,
      filterDialogOpen: false,
      dialogFilters: { ...defaultDialogFilters }
    }
  }),
  actions: {
    getDatasets() {
      const returnedDataSets = [
        { application: '193.174.29.5', inbound: 0, outbound: 3.9 },
        { application: '193.168.31.130', inbound: 1.9, outbound: 2.9 },
        { application: '193.174.25.38', inbound: 2, outbound: 2.8 },
        { application: '0.0.243', inbound: 3, outbound: 2.8 },
        { application: '174.56.0.0', inbound: 0.7, outbound: 0.805 },
        { application: '141.30.223.86', inbound: 3, outbound: 0.345 },
        { application: '193.74.29.55', inbound: 1, outbound: 0.038 }
      ]
      this.datasets = returnedDataSets
    },
    createTableChartData() {
      //Dummy Data until BE is hooked up.
      this.exporters.tableChartData = {
        labels: this.datasets.map((row) => row.application),
        datasets: [
          {
            label: 'Inbound',
            data: this.datasets.map((row) => row.inbound),
            barThickness: 13,
            backgroundColor: '#0043A4',
            hidden: this.filters.traffic.selectedItem === 'outbound'
          },
          {
            label: 'Outbound',
            data: this.datasets.map((row) => row.outbound),
            barThickness: 13,
            backgroundColor: '#EE7D00',
            hidden: this.filters.traffic.selectedItem === 'inbound'
          }
        ]
      }
      this.applications.tableChartData = {
        labels: this.datasets.map((row) => row.application),
        datasets: [
          {
            label: 'Inbound',
            data: this.datasets.map((row) => row.inbound),
            barThickness: 13,
            backgroundColor: '#0043A4',
            hidden: this.filters.traffic.selectedItem === 'outbound'
          },
          {
            label: 'Outbound',
            data: this.datasets.map((row) => row.outbound),
            barThickness: 13,
            backgroundColor: '#EE7D00',
            hidden: this.filters.traffic.selectedItem === 'inbound'
          }
        ]
      }
    },
    filterDialogToggle(event: Event, isAppFilter: boolean) {
      isAppFilter
        ? (this.applications.filterDialogOpen = !this.applications.filterDialogOpen)
        : (this.exporters.filterDialogOpen = !this.exporters.filterDialogOpen)
    },
    generateTableChart() {
      this.getDatasets()
      this.createTableChartData()
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
    trafficRadioOnChange(selectedItem: string) {
      this.filters.traffic.selectedItem = selectedItem
      this.generateTableChart()
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
