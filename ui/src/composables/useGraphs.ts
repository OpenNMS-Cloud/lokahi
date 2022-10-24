import { useQuery } from 'villus'
import { GetMetricDocument, } from '@/types/graphql'
import { DataSets } from '@/types/graphs'

export const useGraphs = () => {
  const variables = ref({ metric: '' })
  const dataSetsObject = reactive({} as any)
  
  const getMetrics = async (metrics: string[]) => {
    for (const metric of metrics) {
      variables.value = { metric }
      await getMetric()
      if (data.value) {
        dataSetsObject[data.value.metric?.data?.result?.[0].metric.__name__] = data.value.metric?.data?.result
      }
    }
  }

  const { data, execute: getMetric } = useQuery({
    query: GetMetricDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables
  })

  return {
    getMetrics,
    dataSets: computed<DataSets>(() => Object.values(dataSetsObject))
  }
}
