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
  FindAllNodesByTagsDocument,
  FindAllNodesByMonitoredStateDocument
} from '@/types/graphql'
import useSpinner from '@/composables/useSpinner'
import { DetectedNode, Monitor, MonitoredStates, MonitoredNode, UnmonitoredNode } from '@/types'

export const useInventoryQueries = defineStore('inventoryQueries', () => {
  const nodes = ref<MonitoredNode[]>([])
  const unmonitoredNodes = ref<UnmonitoredNode[]>([])
  const detectedNodes = ref<DetectedNode[]>([])
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
    cachePolicy: 'network-only',
    fetchOnMount: false
  })

  // Get unmonitored nodes
  const {
    onData: onGetUnmonitoredNodes,
    isFetching: unmonitoredNodesFetching,
    execute: getUnmonitoredNodes
  } = useQuery({
    query: FindAllNodesByMonitoredStateDocument,
    fetchOnMount: false,
    cachePolicy: 'network-only',
    variables: { monitoredState: MonitoredStates.UNMONITORED }
  })

  // Get detected nodes
  const {
    onData: onGetDetectedNodes,
    isFetching: detectedNodesFetching,
    execute: getDetectedNodes
  } = useQuery({
    query: FindAllNodesByMonitoredStateDocument,
    fetchOnMount: false,
    cachePolicy: 'network-only',
    variables: { monitoredState: MonitoredStates.DETECTED }
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
  watchEffect(() => (unmonitoredNodesFetching.value ? startSpinner() : stopSpinner()))
  watchEffect(() => (detectedNodesFetching.value ? startSpinner() : stopSpinner()))

  onData((data) => formatData(data.findAllNodes ?? []))
  onFilteredByLabelData((data) => formatData(data.findAllNodesByNodeLabelSearch ?? []))
  onFilteredByTagsData((data) => formatData(data.findAllNodesByTags ?? []))
  onGetUnmonitoredNodes((data) => formatUnmonitoredNodes(data.findAllNodesByMonitoredState ?? []))
  onGetDetectedNodes((data) => formatDetectedNodes(data.findAllNodesByMonitoredState ?? []))

  const getTagsForData = async (data: Partial<Node>[]) => {
    variables.nodeIds = data.map((node) => node.id)
    await getTags()
  }

  const formatData = async (data: Partial<Node>[]) => {
    nodes.value = []
    if (!data.length) return

    await getTagsForData(data)

    data.forEach(async ({ id, nodeLabel, location, ipInterfaces }) => {
      const { ipAddress: snmpPrimaryIpAddress } = ipInterfaces?.filter((ii) => ii.snmpPrimary)[0] ?? {} // not getting ipAddress from snmpPrimary interface can result in missing metrics for ICMP
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
            locationValue: location?.location ?? '--',
            locationLink: '',
            managementIpValue: '',
            managementIpLink: '',
            tagValue: tagsObj?.tags ?? []
          },
          isNodeOverlayChecked: false,
          type: MonitoredStates.MONITORED
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
        const { ipAddress: snmpPrimaryIpAddress } = ipInterfaces?.filter((ii) => ii.snmpPrimary)[0] ?? {} // not getting ipAddress from snmpPrimary interface can result in missing metrics for ICMP

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
              status: status ?? ''
            }
          ],
          anchor: {
            profileValue: '--',
            profileLink: '',
            locationValue: nodeLocation ?? '--',
            locationLink: '',
            managementIpValue: snmpPrimaryIpAddress ?? '',
            managementIpLink: '',
            tagValue: tagsObj?.tags ?? []
          },
          isNodeOverlayChecked: false,
          type: MonitoredStates.MONITORED
        })
      }
    })
  }

  const formatUnmonitoredNodes = async (data: Partial<Node>[]) => {
    unmonitoredNodes.value = []
    if (!data.length) return

    await getTagsForData(data)

    data.forEach(({ id, nodeLabel, location }) => {
      const tagsObj = tagData.value?.tagsByNodeIds?.filter((item) => item.nodeId === id)[0]
      unmonitoredNodes.value.push({
        id: id,
        label: nodeLabel!,
        anchor: {
          locationValue: location?.location ?? '--',
          tagValue: tagsObj?.tags ?? []
        },
        isNodeOverlayChecked: false,
        type: MonitoredStates.UNMONITORED
      })
    })
  }

  const formatDetectedNodes = async (data: Partial<Node>[]) => {
    detectedNodes.value = []
    if (!data.length) return

    await getTagsForData(data)

    data.forEach(({ id, nodeLabel, location }) => {
      const tagsObj = tagData.value?.tagsByNodeIds?.filter((item) => item.nodeId === id)[0]
      detectedNodes.value.push({
        id: id,
        label: nodeLabel!,
        anchor: {
          locationValue: location?.location ?? '--',
          tagValue: tagsObj?.tags ?? []
        },
        isNodeOverlayChecked: false,
        type: MonitoredStates.DETECTED
      })
    })
  }

  return {
    nodes,
    unmonitoredNodes,
    detectedNodes,
    fetch: execute,
    getNodesByLabel,
    getNodesByTags,
    getUnmonitoredNodes,
    getDetectedNodes
  }
})
