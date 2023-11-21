import { defineStore } from 'pinia'
import { useDashboardQueries } from '@/store/Queries/dashboardQueries'
import { TsResult, TopNNodesQueryVariables, TimeRangeUnit, DownloadTopNQueryVariables, DownloadFormat } from '@/types/graphql'
import { createAndDownloadBlobFile } from '@/components/utils'

type TState = {
  totalNetworkTrafficIn: [number, number][]
  totalNetworkTrafficOut: [number, number][],
  topNodes: any[],
  reachability: { responding: number, unresponsive: number },
  topNNodesQueryVariables: Required<TopNNodesQueryVariables>,
  totalNodeCount: number
}

export const useDashboardStore = defineStore('dashboardStore', {
  state: (): TState => ({
    totalNetworkTrafficIn: [],
    totalNetworkTrafficOut: [],
    topNodes: [],
    reachability: {
      responding: 0,
      unresponsive: 0
    },
    topNNodesQueryVariables: {
      timeRange: 24,
      timeRangeUnit: TimeRangeUnit.Hour,
      sortAscending: true,
      pageSize: 4,
      sortBy: 'reachability',
      page: 1
    },
    totalNodeCount: 0
  }),
  actions: {
    async getNetworkTrafficInValues() {
      const queries = useDashboardQueries()
      await queries.getNetworkTrafficInMetrics()
      this.totalNetworkTrafficIn = (queries.networkTrafficIn as TsResult).metric?.data?.result[0]?.values || []
    },
    async getNetworkTrafficOutValues() {
      const queries = useDashboardQueries()
      await queries.getNetworkTrafficOutMetrics()
      this.totalNetworkTrafficOut = (queries.networkTrafficOut as TsResult).metric?.data?.result[0]?.values || []
    },
    async getNodeCount() {
      const queries = useDashboardQueries()
      this.totalNodeCount = await queries.getNodeCount()
    },
    async getTopNNodes() {
      const queries = useDashboardQueries()
      this.topNodes = await queries.getTopNodes(this.topNNodesQueryVariables)
      this.reachability.responding = this.topNodes.filter((n) => n.avgResponseTime > 0).length
      this.reachability.unresponsive = this.topNodes.length - this.reachability.responding
    },
    async downloadTopNNodesToCsv() {
      const queries = useDashboardQueries()

      const downloadTopNQueryVariables: DownloadTopNQueryVariables = {
        downloadFormat: DownloadFormat.Csv,
        timeRange: this.topNNodesQueryVariables.timeRange,
        timeRangeUnit: this.topNNodesQueryVariables.timeRangeUnit,
        sortAscending: this.topNNodesQueryVariables.sortAscending,
        sortBy: this.topNNodesQueryVariables.sortBy
      }

      const bytes = await queries.downloadTopNodes(downloadTopNQueryVariables)
      createAndDownloadBlobFile(bytes, 'TopNodes.csv')
    },
    setTopNNodesTablePage(page: number) {
      if (page !== this.topNNodesQueryVariables.page) {
        this.topNNodesQueryVariables = {
          ...this.topNNodesQueryVariables,
          page
        }
      }
  
      this.getTopNNodes()
    },
    setTopNNodesTableSort(sortObj: Record<string, string>) {
      const isAscending = sortObj.value === 'asc'

      this.topNNodesQueryVariables = {
        ...this.topNNodesQueryVariables,
        sortAscending: isAscending,
        sortBy: sortObj.property
      }

      this.getTopNNodes()
    }
  }
})
