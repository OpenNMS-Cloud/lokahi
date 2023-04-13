import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import {
  NodesListDocument,
  NodeLatencyMetricDocument,
  TsResult,
  Location,
  TimeRangeUnit,
  ListTagsByNodeIdsDocument,
  FindAllNodesByNodeLabelSearchDocument,
  Node,
  FindAllNodesByTagsDocument
} from '@/types/graphql'
import { NodeContent } from '@/types/inventory'
import useSpinner from '@/composables/useSpinner'
import { Monitor } from '@/types'

export const useInventoryQueries = defineStore('inventoryQueries', () => {
  const nodes = ref<NodeContent[]>([])
  const variables = reactive({ nodeIds: <number[]>[] })
  const labelSearchVariables = reactive({ labelSearchTerm: '' })
  const tagsVariables = reactive({ tags: <string[]>[] })

  const { startSpinner, stopSpinner } = useSpinner()

  // Get all nodes
  const {
    onData,
    isFetching: nodesFetching,
    execute
  } = useQuery({
    query: NodesListDocument,
    cachePolicy: 'network-only'
  })

  // Get nodes by label
  const {
    onData: onFilteredByLabelData,
    isFetching: filteredNodesByLabelFetching,
    execute: filterNodesByLabel
  } = useQuery({
    query: FindAllNodesByNodeLabelSearchDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables: labelSearchVariables
  })

  const getNodesByLabel = (label: string) => {
    labelSearchVariables.labelSearchTerm = label
    filterNodesByLabel()
  }

  // Get nodes by tags
  const {
    onData: onFilteredByTagsData,
    isFetching: filteredNodesByTagsFetching,
    execute: filterNodesByTags
  } = useQuery({
    query: FindAllNodesByTagsDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables: tagsVariables
  })

  const getNodesByTags = (tags: string[]) => {
    tagsVariables.tags = tags
    filterNodesByTags()
  }

  // Get tags for nodes
  const { data: tagData, execute: getTags } = useQuery({
    query: ListTagsByNodeIdsDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables
  })

  // Get metrics for specific node
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
      cachePolicy: 'network-only'
    })

  watchEffect(() => (nodesFetching.value ? startSpinner() : stopSpinner()))
  watchEffect(() => (filteredNodesByLabelFetching.value ? startSpinner() : stopSpinner()))
  watchEffect(() => (filteredNodesByTagsFetching.value ? startSpinner() : stopSpinner()))

  onData((data) => formatData(data.findAllNodes || []))
  onFilteredByLabelData((data) => formatData(data.findAllNodesByNodeLabelSearch || []))
  onFilteredByTagsData((data) => formatData(data.findAllNodesByTags || []))

  const formatData = async (data: Partial<Node>[]) => {
    nodes.value = []

    if (data?.length) {
      // get the tags for all nodeIds
      variables.nodeIds = data.map((node) => node.id)
      await getTags()

      data.forEach(async ({ id, nodeLabel, location, ipInterfaces }) => {
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
  }

  return {
    nodes,
    fetch: execute,
    getNodesByLabel,
    getNodesByTags
  }
})
