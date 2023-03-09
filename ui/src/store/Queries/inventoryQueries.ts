import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import {
  NodesListDocument,
  NodeLatencyMetricDocument,
  TsResult,
  Location,
  TimeRangeUnit,
  ListTagsByNodeIdDocument
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

  const fetchNodeTags = (nodeId: number) =>
    useQuery({
      query: ListTagsByNodeIdDocument,
      variables: {
        nodeId
      }
    })

  watchEffect(() => {
    nodes.value = []

    const allNodes = nodesData.value?.findAllNodes

    if (allNodes?.length) {
      allNodes.forEach(async ({ id, nodeLabel, location, ipInterfaces }) => {
        const { ipAddress: snmpPrimaryIpAddress } = ipInterfaces?.filter((ii) => ii.snmpPrimary)[0] || {} // not getting ipAddress from snmpPrimary interface can result in missing metrics for ICMP

        // stop-gap measure to display nodes without IP addresses
        // may be removed once BE disassociates instance with IP
        if (!snmpPrimaryIpAddress) {
          nodes.value.push({
            id: id,
            label: nodeLabel,
            status: '',
            metrics: [
              {
                type: 'latency',
                label: 'Latency',
                value: 0,
                status: ''
              },
              {
                type: 'status',
                label: 'Status',
                status: 'NO IP'
              }
            ],
            anchor: {
              profileValue: '--',
              profileLink: '',
              locationValue: location?.location || '--',
              locationLink: '',
              managementIpValue: '',
              managementIpLink: '',
              tagValue: []
            },
            isNodeOverlayChecked: false
          })
          return
        }

        const [{ data: metricData, isFetching: metricFetching }, { data: tagData, isFetching: tagFetching }] =
          await Promise.all([fetchNodeMetrics(id, snmpPrimaryIpAddress as string), fetchNodeTags(id)])

        if (!metricFetching.value && metricData.value && !tagFetching.value && tagData.value) {
          const nodeLatency = metricData.value.nodeLatency?.data?.result as TsResult[]
          const latenciesValues = [...nodeLatency][0]?.values as number[][]
          // get the last item of the list
          const latencyValue = latenciesValues?.length ? latenciesValues[latenciesValues.length - 1][1] : undefined

          const status = metricData.value.nodeStatus?.status
          const { location: nodeLocation } = location as Location
          const { ipAddress: snmpPrimaryIpAddress } = ipInterfaces?.filter((ii) => ii.snmpPrimary)[0] || {} // not getting ipAddress from snmpPrimary interface can result in missing metrics for ICMP

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
              managementIpValue: snmpPrimaryIpAddress || '',
              managementIpLink: '',
              tagValue: tagData.value.tagsByNodeId || []
            },
            isNodeOverlayChecked: false // to control the checkmark in the overlay of a node (tagging mode)
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
