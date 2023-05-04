import { useQuery } from 'villus'
import { GetTimeSeriesMetricDocument } from '@/types/graphql'
import { DataSets, MetricArgs, GraphProps } from '@/types/graphs'

export const useGraphs = () => {
  const variables = ref({} as MetricArgs)
  const dataSetsObject = reactive({} as any)

  const { data, execute: getMetric } = useQuery({
    query: GetTimeSeriesMetricDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables
  })

  const getMetrics = async (props: GraphProps) => {
    const { metrics, monitor, timeRange, timeRangeUnit, instance, nodeId } = props

    for (const metricStr of metrics) {
      variables.value = { name: metricStr, monitor, timeRange, timeRangeUnit, instance, nodeId }
      await getMetric()

      const result = data.value?.metric?.data?.result?.[0]

      if (result) {
        const { metric, values } = result

        if (values?.length) {
          dataSetsObject[metricStr] = {
            metric,
            values: values.filter((val) => {
              const [timestamp, value] = val
              if (timestamp && value) return val
            })
          }
        }
      }
    }
  }

  return {
    getMetrics,
    dataSets: computed<DataSets>(() => Object.values(dataSetsObject))
  }
}
