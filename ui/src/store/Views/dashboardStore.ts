import { defineStore } from 'pinia'
import { useDashboardQueries } from '@/store/Queries/dashboardQueries'
import { TsResult } from '@/types/graphql'

type TState = {
  totalNetworkTrafficIn: [number, number][]
  totalNetworkTrafficOut: [number, number][]
}

export const useDashboardStore = defineStore('dashboardStore', {
  state: (): TState => ({
    totalNetworkTrafficIn: [],
    totalNetworkTrafficOut: []
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
