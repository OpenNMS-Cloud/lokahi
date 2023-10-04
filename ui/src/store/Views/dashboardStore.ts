import { defineStore } from 'pinia'
import { useDashboardQueries } from '@/store/Queries/dashboardQueries'
import { TsResult, Node } from '@/types/graphql'

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
    topNodes: [{ id: 1, nodeLabel: 'test', monitoringLocationId: 1, location: {location: 'loc1'}, responseTime: 123, reachability: 90}],
    reachability: {
      responding: 8,
      unresponsive: 3
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
    }
  }
})
