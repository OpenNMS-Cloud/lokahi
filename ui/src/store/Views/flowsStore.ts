import { format } from 'date-fns'
import { defineStore } from 'pinia'
import { useflowsQueries } from '@/store/Queries/flowsQueries'
import { RequestCriteriaInput, TimeRange } from '@/types/graphql'
import { FlowsApplicationSummaries } from '@/types'
import { IAutocompleteItemType } from '@featherds/autocomplete/src/components/types'
import { useFlowsApplicationStore } from './flowsApplicationStore'

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
      maxDataPoints: undefined as undefined | number,
      //Application AutoComplete
      applications: [] as IAutocompleteItemType[],
      selectedApplications: [],
      isApplicationsLoading: false,
      filteredApplications: [] as IAutocompleteItemType[],
      //Exporter AutoComplete
      exporters: [] as IAutocompleteItemType[],
      selectedExporterTopApplication: undefined as undefined | IAutocompleteItemType,
      // Selected Exporters can be set as [{ _text: 'Node Name', value: { nodeId: 1, ipInterfaceId: 1 } }] to autopopulate autofill with exporter
      selectedExporters: [] as IAutocompleteItemType[],
      isExportersLoading: false,
      filteredExporters: [] as IAutocompleteItemType[]
    }
  }),
  actions: {
    async getApplicationDatasets() {
      const flowsApplicationStore = useFlowsApplicationStore()
      const requestData = this.getRequestData()
      await flowsApplicationStore.getApplicationTableDataset(requestData)
      await flowsApplicationStore.getApplicationLineDataset(requestData)
    },
    async getApplications() {
      const flowsQueries = useflowsQueries()
      const requestData = this.getRequestData(50, [], [])

      const applications = (await flowsQueries.getApplications(requestData)) || []
      const applicationsAutocompleteObject = applications.value?.findApplications?.map((item: string) => ({
        _text: item.toUpperCase(),
        value: item
      })) as IAutocompleteItemType[]
      this.filters.applications = applicationsAutocompleteObject
    },
    async getExporters() {
      const flowsQueries = useflowsQueries()
      const requestData = this.getRequestData(undefined, [], [])

      const exporters = (await flowsQueries.getExporters(requestData)) || []
      const exportersAutocompleteObject = exporters.value?.findExporters?.map((item: any) => ({
        _text: `${item.node.nodeLabel.toUpperCase()} : ${item.ipInterface.ipAddress}`,
        value: { nodeId: item.node.id as number, ipInterfaceId: item.ipInterface.id as number }
      })) as IAutocompleteItemType[]
      this.filters.exporters = exportersAutocompleteObject
    },
    getRequestData(count = 10, exporter?: object[], applications?: string[]) {
      const timeRange = this.getTimeRange(this.filters.dateFilter)
      const step = this.calculateStep(timeRange.startTime, timeRange.endTime, this.filters.maxDataPoints)

      return {
        count,
        step,
        exporter: exporter || this.filters.selectedExporters.map((exp: any) => exp.value),
        timeRange,
        applications: applications || this.filters.selectedApplications.map((app: any) => app.value)
      } as RequestCriteriaInput
    },
    createApplicationCharts() {
      const flowsApplicationStore = useFlowsApplicationStore()
      flowsApplicationStore.createApplicationTableChartData()
      flowsApplicationStore.createApplicationLineChartData()
    },
    async updateApplicationCharts() {
      await this.getApplicationDatasets()
      this.createApplicationCharts()
    },
    async populateData() {
      await this.getExporters()
      await this.getApplications()
      await this.updateApplicationCharts()
    },
    async updateChartData() {
      await this.updateApplicationCharts()
    },
    async trafficRadioOnChange(selectedItem: string) {
      this.filters.traffic.selectedItem = selectedItem
      this.createApplicationCharts()
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
    getSpanGap() {
      switch (this.filters.dateFilter) {
        case TimeRange.Today:
          return 1000 * 60 * 5 // 5 mins 
        case TimeRange.Last_24Hours:
          return 1000 * 60 * 10 // 10 mins 
        case TimeRange.SevenDays:
          return 1000 * 60 * 60 // 1 hour 
        default:
          return 1000 * 60 * 5 // 5 mins 
      }
    },
    async onDateFilterUpdate(e: any) {
      this.filters.dateFilter = e
      await this.updateChartData()
    },
    calculateStep(start: number, end: number, maxDataPoints?: number) {
      if (!maxDataPoints) return
      return Math.floor((end - start) / maxDataPoints)
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
    }
  }
})
