import { defineStore } from 'pinia'
import { useDashboardQueries } from '@/store/Queries/dashboardQueries'
import { TsResult } from '@/types/graphql'

type TState = {
  totalNetworkTrafficIn: [number, number][]
  totalNetworkTrafficOut: [number, number][],
  topNodes: any[],
  reachability: Record<string, number>
}

export const useDashboardStore = defineStore('dashboardStore', {
  state: (): TState => ({
    totalNetworkTrafficIn: [],
    totalNetworkTrafficOut: [],
    topNodes: [],
    reachability: {
      responding: 0,
      unresponsive: 0
    }
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
    async getTopNNodes() {
      const queries = useDashboardQueries()
      await queries.getTopNodes()
      this.topNodes = queries.topNodes

      this.reachability.responding = queries.topNodes.filter((n) => n.avgResponseTime > 0).length
      this.reachability.unchrachable = queries.topNodes.length - this.reachability.responding
    }
  }
})
