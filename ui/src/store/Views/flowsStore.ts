import { flowApptoChartJSLine } from '@/dtos/chartJS.dto'
import { ChartData } from 'chart.js'
import { format } from 'date-fns'
import { defineStore } from 'pinia'

export const useFlowsStore = defineStore('flowsStore', {
  state: () => ({
    tableDatasets: [{} as any],
    lineDatasets: [{} as any],
    tableChartOptions: {},
    totalFlows: '1,957',
    filters: {
      dateFilter: 'today',
      traffic: {
        selectedItem: 'total'
      },
      dataStyle: {
        selectedItem: 'table'
      }
    },
    applications: {
      tableChartData: {} as ChartData,
      lineChartData: {} as ChartData,
      expansionOpen: true,
      filterDialogOpen: false,
      dialogFilters: { ...defaultDialogFilters }
    },
    exporters: {
      tableChartData: {} as ChartData,
      lineChartData: {} as ChartData,
      expansionOpen: true,
      filterDialogOpen: false,
      dialogFilters: { ...defaultDialogFilters }
    }
  }),
  actions: {
    getDatasets() {
      //Will be replaced with a BE call
      const returnedTableDataSets = [
        {
          label: 'app_0',
          bytesIn: 407371,
          bytesOut: 402374
        },
        {
          label: 'app_1',
          bytesIn: 63491,
          bytesOut: 62750
        },
        {
          label: 'app_2',
          bytesIn: 95187,
          bytesOut: 19754
        },
        {
          label: 'app_3',
          bytesIn: 76824,
          bytesOut: 95025
        },
        {
          label: 'app_4',
          bytesIn: 16870,
          bytesOut: 97879
        },
        {
          label: 'app_5',
          bytesIn: 86697,
          bytesOut: 90904
        },
        {
          label: 'app_6',
          bytesIn: 10761,
          bytesOut: 71282
        },
        {
          label: 'app_7',
          bytesIn: 67521,
          bytesOut: 86472
        },
        {
          label: 'app_8',
          bytesIn: 37723,
          bytesOut: 61793
        },
        {
          label: 'app_9',
          bytesIn: 36712,
          bytesOut: 63233
        }
      ]
      this.tableDatasets = returnedTableDataSets

      //Will be replaced with a BE call
      const returnedLineDataSets = [
        {
          timestamp: '2023-01-10T01:01:25Z',
          label: 'app_0',
          value: 138790.1750375429,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:09:47Z',
          label: 'app_0',
          value: 216861.9119974472,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:18:07Z',
          label: 'app_0',
          value: 202966.96568806906,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:26:27Z',
          label: 'app_0',
          value: 264173.0346710393,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:34:47Z',
          label: 'app_0',
          value: 28631.814024643267,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:43:07Z',
          label: 'app_0',
          value: 446770.59738684486,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:51:27Z',
          label: 'app_0',
          value: 475634.2969400164,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:59:47Z',
          label: 'app_0',
          value: 291804.2768482603,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T02:08:07Z',
          label: 'app_0',
          value: 192281.57332635063,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T02:16:27Z',
          label: 'app_0',
          value: 781683.5022437341,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:01:27Z',
          label: 'app_1',
          value: 446316.3858784942,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:09:47Z',
          label: 'app_1',
          value: 675859.8717571729,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:18:07Z',
          label: 'app_1',
          value: 703441.1031400502,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:26:27Z',
          label: 'app_1',
          value: 277710.0783393889,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:34:47Z',
          label: 'app_1',
          value: 928133.4566595472,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:43:07Z',
          label: 'app_1',
          value: 299538.99195372575,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:51:27Z',
          label: 'app_1',
          value: 264867.46362876357,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T01:59:47Z',
          label: 'app_1',
          value: 738735.9705331036,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T02:08:07Z',
          label: 'app_1',
          value: 761950.6966375934,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-01-10T02:16:27Z',
          label: 'app_1',
          value: 995884.662063265,
          direction: 'EGRESS'
        },
        {
          timestamp: '2023-03-24T00:40:37Z',
          label: 'ipsec-nat-t',
          value: 798,
          direction: 'INGRESS'
        },
        {
          timestamp: '2023-03-24T01:40:37Z',
          label: 'ipsec-nat-t',
          value: 9133508,
          direction: 'INGRESS'
        }
      ]

      this.lineDatasets = flowApptoChartJSLine(returnedLineDataSets)
    },
    createTableChartData() {
      //Dummy Data until BE is hooked up.
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
    },
    createLineChartData() {
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
    generateLineChart() {
      this.getDatasets()
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
    trafficRadioOnChange(selectedItem: string) {
      this.filters.traffic.selectedItem = selectedItem
      this.generateTableChart()
    },
    convertToDate(ts: string) {
      const dateFormat = () => {
        switch (this.filters.dateFilter) {
          case 'today':
            return 'HH:mm'
          case '24h':
            return 'HH:mm'
          case '7d':
            return 'dd/MMM HH:mm'
          default:
            return 'dd/MMM HH:mm'
        }
      }
      return format(new Date(ts), dateFormat())
    },
    onDateFilterUpdate(e: any) {
      this.filters.dateFilter = e
      this.createLineChartData()
      this.createTableChartData()
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
