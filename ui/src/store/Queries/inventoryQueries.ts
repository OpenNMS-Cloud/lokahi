import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import {
  NodesListDocument,
  NodeLatencyMetricDocument,
  TsResult,
  IpInterface,
  Location,
  TimeRangeUnit
} from '@/types/graphql'
import { NodeContent } from '@/types/inventory'
import useSpinner from '@/composables/useSpinner'
import { Monitor } from '@/types'

export const useInventoryQueries = defineStore('inventoryQueries', () => {
  const nodes = ref<NodeContent[]>([])

  const { startSpinner, stopSpinner } = useSpinner()

  const {
    data: nodesData,
    isFetching: nodesFetching,
    execute
  } = useQuery({
    query: NodesListDocument,
    cachePolicy: 'network-only' // always fetch and do not cache
  })

  const fetchNodeMetrics = (id: number, instance: string) =>
    useQuery({
      query: NodeLatencyMetricDocument,
      variables: {
        id,
        instance,
        monitor: Monitor.ICMP,
        timeRange: 1,
        timeRangeUnit: TimeRangeUnit.Minute
      },
      cachePolicy: 'network-only' // always fetch and do not cache
    })

  watch(nodesFetching, (_, fetched) => (fetched ? stopSpinner() : startSpinner()))

  watchEffect(() => {
    nodes.value = []

    const allNodes = nodesData.value?.findAllNodes

    if (allNodes?.length) {
      allNodes.forEach(async ({ id, nodeLabel, location, ipInterfaces }) => {
        const { data, isFetching } = await fetchNodeMetrics(id, ipInterfaces?.[0].ipAddress as string) // currently only 1 interface per node

        if (data.value && !isFetching.value) {
          const nodeLatency = data.value.nodeLatency?.data?.result as TsResult[]
          const latenciesValues = [...nodeLatency][0]?.values as number[][]
          // get the last item of the list
          const latencyValue = latenciesValues?.length ? latenciesValues[latenciesValues.length - 1][1] : undefined

          const status = data.value.nodeStatus?.status
          const { location: nodeLocation } = location as Location
          const [{ ipAddress }] = ipInterfaces as IpInterface[]

          nodes.value.push({
            id: id,
            label: nodeLabel,
            status: '',
            metrics: [
              {
                type: 'latency',
                label: 'Latency',
                value: latencyValue,
                status: ''
              },
              {
                type: 'status',
                label: 'Status',
                status: status || ''
              }
            ],
            anchor: {
              profileValue: '--',
              profileLink: '',
              locationValue: nodeLocation || '--',
              locationLink: '',
              managementIpValue: ipAddress || '',
              managementIpLink: '',
              tagValue: '--',
              tagLink: ''
            }
          })
        }
      })
    }
  })

  return {
    nodes,
    fetch: execute
  }
})
