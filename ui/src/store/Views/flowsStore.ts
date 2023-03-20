import { ChartData } from 'chart.js'
import { defineStore } from 'pinia'

export const useFlowsStore = defineStore('flowsStore', {
  state: () => ({
    datasets: [{} as any],
    tableChartData: {} as ChartData,
    tableChartOptions: {},
    dateFilter: '2023-1-14 to 2023-1-15'
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
      this.tableChartData = {
        labels: this.datasets.map((row) => row.application),
        datasets: [
          {
            label: 'Inbound',
            data: this.datasets.map((row) => row.inbound),
            barThickness: 13,
            backgroundColor: '#0043A4'
          },
          {
            label: 'Outbound',
            data: this.datasets.map((row) => row.outbound),
            barThickness: 13,
            backgroundColor: '#EE7D00'
          }
        ]
      }
    },
    generateTableChart() {
      this.getDatasets()
      this.createTableChartData()
    }
  }
})
