import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import {
  NodesListDocument,
  NodeLatencyMetricDocument,
  TsResult,
  TimeRangeUnit,
  ListTagsByNodeIdsDocument,
  FindAllNodesByNodeLabelSearchDocument,
  Node,
  FindAllNodesByTagsDocument,
  FindAllNodesByMonitoredStateDocument,
  NodeTags,
  NodeLatencyMetricQuery
} from '@/types/graphql'
import useSpinner from '@/composables/useSpinner'
import { DetectedNode, Monitor, MonitoredStates, MonitoredNode, UnmonitoredNode, AZURE_SCAN } from '@/types'

export const useInventoryQueries = defineStore('inventoryQueries', () => {
  const nodes = ref<MonitoredNode[]>([])
  const unmonitoredNodes = ref<UnmonitoredNode[]>([])
  const detectedNodes = ref<DetectedNode[]>([])
  const variables = reactive({ nodeIds: <number[]>[] })
  const labelSearchVariables = reactive({ labelSearchTerm: '' })
  const tagsVariables = reactive({ tags: <string[]>[] })
  const metricsVariables = reactive({
    id: 0,
    instance: '',
    monitor: Monitor.ICMP,
    timeRange: 1,
    timeRangeUnit: TimeRangeUnit.Minute
  })

  const { startSpinner, stopSpinner } = useSpinner()

  // Get all nodes - cannot yet specify for monitored nodes
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

  // Get nodes by label - cannot yet filter by monitored state
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

  // Get nodes by tags - cannot yet filter by monitored state
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
  const {
    data: tagData,
    execute: getTags,
    onData: onTagsData
  } = useQuery({
    query: ListTagsByNodeIdsDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables
  })

  // Get metrics for nodes
  const { onData: onMetricsData, execute: getMetrics } = useQuery({
    query: NodeLatencyMetricDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables: metricsVariables
  })

  watchEffect(() => (nodesFetching.value ? startSpinner() : stopSpinner()))
  watchEffect(() => (filteredNodesByLabelFetching.value ? startSpinner() : stopSpinner()))
  watchEffect(() => (filteredNodesByTagsFetching.value ? startSpinner() : stopSpinner()))
  watchEffect(() => (unmonitoredNodesFetching.value ? startSpinner() : stopSpinner()))
  watchEffect(() => (detectedNodesFetching.value ? startSpinner() : stopSpinner()))

  onData((data) => formatMonitoredNodes(data.findAllNodes ?? []))
  onFilteredByLabelData((data) => formatMonitoredNodes(data.findAllNodesByNodeLabelSearch ?? []))
  onFilteredByTagsData((data) => formatMonitoredNodes(data.findAllNodesByTags ?? []))
  onGetUnmonitoredNodes((data) => formatUnmonitoredNodes(data.findAllNodesByMonitoredState ?? []))
  onGetDetectedNodes((data) => formatDetectedNodes(data.findAllNodesByMonitoredState ?? []))
  onMetricsData((data) => addMetricsToMonitoredNodes(data))
  onTagsData((data) => addTagsToMonitoredNodes(data.tagsByNodeIds ?? []))

  // sets the initial monitored node object and then calls for tags and metrics
  const formatMonitoredNodes = async (data: Partial<Node>[]) => {
    nodes.value = []
    if (!data.length) return
    setMonitoredNodes(data)
    await getTagsForData(data)
    await getMetricsForData(data)
  }

  // sets the initial monitored node object
  const setMonitoredNodes = (data: Partial<Node>[]) => {
    for (const node of data) {
      const { ipAddress: snmpPrimaryIpAddress } = node.ipInterfaces?.filter((x) => x.snmpPrimary)[0] ?? {}
      const monitoredNode: MonitoredNode = {
        id: node.id,
        label: node.nodeLabel,
        status: '',
        metrics: [],
        anchor: {
          profileValue: '--',
          profileLink: '',
          locationValue: node.location?.location ?? '--',
          locationLink: '',
          managementIpValue: snmpPrimaryIpAddress ?? '',
          managementIpLink: '',
          tagValue: []
        },
        isNodeOverlayChecked: false,
        type: MonitoredStates.MONITORED
      }

      nodes.value.push(monitoredNode)
    }
  }

  // may be used to get tags for monitored/unmonitored/detected nodes
  const getTagsForData = async (data: Partial<Node>[]) => {
    variables.nodeIds = data.map((node) => node.id)
    await getTags()
  }

  // may only be used for monitored nodes
  const getMetricsForData = async (data: Partial<Node>[]) => {
    for (const node of data) {
      const { ipAddress: snmpPrimaryIpAddress } = node.ipInterfaces?.filter((x) => x.snmpPrimary)[0] ?? {}
      const instance = node.scanType === AZURE_SCAN ? `azure-node-${node.id}` : snmpPrimaryIpAddress!

      metricsVariables.id = node.id
      metricsVariables.instance = instance
      await getMetrics()
    }
  }

  // callback after getTags call complete
  const addTagsToMonitoredNodes = (tags: NodeTags[]) => {
    nodes.value = nodes.value.map((node) => {
      tags.forEach((tag) => {
        if (node.id === tag.nodeId) {
          node.anchor.tagValue = tag.tags ?? []
        }
      })
      
      return node
    })
  }

  // callback after getMetrics call complete
  const addMetricsToMonitoredNodes = (data: NodeLatencyMetricQuery) => {
    const latency = data.nodeLatency
    const status = data.nodeStatus

    const nodeId = latency?.data?.result?.[0]?.metric?.node_id || status?.id

    if (!nodeId) {
      console.warn('Cannot obtain metrics: No node id')
      return
    }

    const nodeLatency = latency?.data?.result as TsResult[]
    const latenciesValues = [...nodeLatency][0]?.values as number[][]
    const latencyValue = latenciesValues?.length ? latenciesValues[latenciesValues.length - 1][1] : undefined

    nodes.value = nodes.value.map((node) => {
      if (node.id === Number(nodeId)) {
        node.metrics = [
          {
            type: 'latency',
            label: 'Latency',
            value: latencyValue,
            status: ''
          },
          {
            type: 'status',
            label: 'Status',
            status: status?.status ?? ''
          }
        ]
      }

      return node
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
