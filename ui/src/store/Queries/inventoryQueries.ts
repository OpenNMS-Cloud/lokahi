import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import {
  NodesListDocument,
  NodeLatencyMetricDocument,
  TsResult,
  Location,
  TimeRangeUnit,
  ListTagsByNodeIdsDocument
} from '@/types/graphql'
import { NodeContent } from '@/types/inventory'
import useSpinner from '@/composables/useSpinner'
import { Monitor } from '@/types'

export const useInventoryQueries = defineStore('inventoryQueries', () => {
  const nodes = ref<NodeContent[]>([])
  const variables = reactive({ nodeIds: <number[]>[] })

  const { startSpinner, stopSpinner } = useSpinner()

  const {
    data: nodesData,
    isFetching: nodesFetching,
    execute
  } = useQuery({
    query: NodesListDocument,
    cachePolicy: 'network-only' // always fetch and do not cache
  })

  const { data: tagData, execute: getTags } = useQuery({
    query: ListTagsByNodeIdsDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables
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

  watchEffect(async () => {
    nodes.value = []

    const allNodes = nodesData.value?.findAllNodes

    if (allNodes?.length) {
      // get the tags for all nodeIds
      variables.nodeIds = allNodes.map((node) => node.id)
      await getTags()

      allNodes.forEach(async ({ id, nodeLabel, location, ipInterfaces }) => {
        const { ipAddress: snmpPrimaryIpAddress } = ipInterfaces?.filter((ii) => ii.snmpPrimary)[0] || {} // not getting ipAddress from snmpPrimary interface can result in missing metrics for ICMP
        const tagsObj = tagData.value?.tagsByNodeIds?.filter((item) => item.nodeId === id)[0]

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
              tagValue: tagsObj?.tags || []
            },
            isNodeOverlayChecked: false
          })
          return
        }

        const [{ data: metricData, isFetching: metricFetching }] = await Promise.all([
          fetchNodeMetrics(id, snmpPrimaryIpAddress as string)
        ])

        if (!metricFetching.value && metricData.value) {
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
              tagValue: tagsObj?.tags || []
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
