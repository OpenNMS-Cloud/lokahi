// import { defineStore } from 'pinia'
// import { useQuery } from 'villus'
// import { UsageStatsReportDocument } from '@/types/graphql'

// export const useUsageStatsQueries = defineStore('usageStatsQueries', () => {
//   const { data, execute } = useQuery({
//     query: UsageStatsReportDocument,
//     cachePolicy: 'network-only',
//     fetchOnMount: false // Prevents unknown error on load. Uncomment this when new usage stats endpoint is integrated
//   })

//   return {
//     usageStatsReport: computed(() => data.value?.usageStatsReport || {}),
//     fetch: execute
//   }
// })
