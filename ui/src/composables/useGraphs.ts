import { useQuery } from 'villus'
import { GetTimeSeriesMetricDocument, GetTimeSeriesMetricsWithIfNameDocument } from '@/types/graphql'
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

  const { data: ifNameData, execute: getMetricsWithIfName } = useQuery({
    query: GetTimeSeriesMetricsWithIfNameDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables
  })

  const getMetrics = async (props: GraphProps) => {
    const { metrics, monitor, timeRange, timeRangeUnit, instance, nodeId, ifName } = props

    let result

    for (const metricStr of metrics) {
      if (instance) {
        variables.value = { name: metricStr, monitor, timeRange, timeRangeUnit, instance, nodeId }
        await getMetric()
        result = data.value?.metric?.data?.result?.[0]

      } else {
        variables.value = { name: metricStr, monitor, timeRange, timeRangeUnit, nodeId, ifName }
        await getMetricsWithIfName()
        result = ifNameData.value?.metric?.data?.result?.[0]
      }

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
