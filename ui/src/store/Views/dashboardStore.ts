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
    topNodes: [
      { id: 1, 
        nodeLabel: 'Windows-Box-1', 
        monitoringLocationId: 1, 
        location: {location: 'Ottawa'}, 
        responseTime: 1072, 
        reachability: 46
      },
      { id: 2, 
        nodeLabel: 'Windows-Box-2', 
        monitoringLocationId: 1, 
        location: {location: 'Ottawa'}, 
        responseTime: 674, 
        reachability: 74
      },
      { id: 3, 
        nodeLabel: 'BOS-Router', 
        monitoringLocationId: 1, 
        location: {location: 'Ottawa'}, 
        responseTime: 135, 
        reachability: 87
      },
      { id: 4, 
        nodeLabel: 'BDU-Router', 
        monitoringLocationId: 1, 
        location: {location: 'Ottawa'}, 
        responseTime: 15, 
        reachability: 100
      },
    ],
    reachability: {
      responding: 3,
      unresponsive: 1
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
