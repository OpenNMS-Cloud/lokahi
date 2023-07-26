import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import {
  FindAllNodesByMonitoredStateDocument,
  FindAllNodesByNodeLabelSearchDocument,
  FindAllNodesByNodeLabelSearchQueryVariables,
  FindAllNodesByTagsDocument,
  FindAllNodesByTagsQueryVariables,
  ListTagsByNodeIdsDocument,
  MonitoredState,
  Node,
  NodeLatencyMetricDocument,
  NodeLatencyMetricQuery,
  NodeTags,
  TimeRangeUnit,
  TsResult
} from '@/types/graphql'
import { AZURE_SCAN, DetectedNode, InventoryNode, Monitor, MonitoredNode, UnmonitoredNode } from '@/types'
import { Chip } from '@/types/metric'

export const useInventoryQueries = defineStore('inventoryQueries', () => {
  const selectedState = ref(MonitoredState.Monitored)
  const monitoredNodes = ref<MonitoredNode[]>([])
  const unmonitoredNodes = ref<UnmonitoredNode[]>([])
  const detectedNodes = ref<DetectedNode[]>([])

  // Get monitored nodes
  const {
    onData: onGetMonitoredNodes,
    isFetching: monitoredNodesFetching,
    execute: getMonitoredNodes
  } = useQuery({
    query: FindAllNodesByMonitoredStateDocument,
    fetchOnMount: false,
    cachePolicy: 'network-only',
    variables: { monitoredState: MonitoredState.Monitored }
  })
  onGetMonitoredNodes(async (data) => monitoredNodes.value = await formatMonitoredNodes(data.findAllNodesByMonitoredState ?? []))

  // Get unmonitored nodes
  const {
    onData: onGetUnmonitoredNodes,
    isFetching: unmonitoredNodesFetching,
    execute: getUnmonitoredNodes
  } = useQuery({
    query: FindAllNodesByMonitoredStateDocument,
    fetchOnMount: false,
    cachePolicy: 'network-only',
    variables: { monitoredState: MonitoredState.Unmonitored }
  })
  onGetUnmonitoredNodes(async (data) => unmonitoredNodes.value = await formatUnmonitoredNodes(data.findAllNodesByMonitoredState ?? []))

  // Get detected nodes
  const {
    onData: onGetDetectedNodes,
    isFetching: detectedNodesFetching,
    execute: getDetectedNodes
  } = useQuery({
    query: FindAllNodesByMonitoredStateDocument,
    fetchOnMount: false,
    cachePolicy: 'network-only',
    variables: { monitoredState: MonitoredState.Detected }
  })
  onGetDetectedNodes(async (data) => detectedNodes.value = await formatDetectedNodes(data.findAllNodesByMonitoredState ?? []))

  // Get nodes by label
  const labelSearchVariables = reactive<FindAllNodesByNodeLabelSearchQueryVariables>(
    { labelSearchTerm: '', monitoredState: MonitoredState.Monitored }
  )
  const {
    isFetching: filteredNodesByLabelFetching,
    execute: filterNodesByLabel
  } = useQuery({
    query: FindAllNodesByNodeLabelSearchDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables: labelSearchVariables
  })

  const getNodesByLabel = async (label: string, monitoredState: MonitoredState) => {
    labelSearchVariables.labelSearchTerm = label
    labelSearchVariables.monitoredState = monitoredState
    const result = await filterNodesByLabel()
    if (result.data) {
      const searchResults = await formatNodes(result.data.findAllNodesByNodeLabelSearch ?? [], labelSearchVariables.monitoredState)
      setByState(monitoredState, searchResults)
    }
  }

  // Get nodes by tags
  const filterNodesByTagsVariables = reactive<FindAllNodesByTagsQueryVariables>(
    { tags: <string[]>[], monitoredState: MonitoredState.Monitored }
  )
  const {
    isFetching: filteredNodesByTagsFetching,
    execute: filterNodesByTags
  } = useQuery({
    query: FindAllNodesByTagsDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables: filterNodesByTagsVariables
  })

  const getNodesByTags = async (tags: string[], monitoredState: MonitoredState) => {
    filterNodesByTagsVariables.tags = tags
    filterNodesByTagsVariables.monitoredState = monitoredState
    const result = await filterNodesByTags()
    if (result.data) {
      const searchResults = await formatNodes(result.data.findAllNodesByTags ?? [], filterNodesByTagsVariables.monitoredState)
      setByState(monitoredState, searchResults)
    }
  }

  // Get tags for nodes
  const listTagsByNodeIdsVariables = reactive({ nodeIds: <number[]>[] })
  const {
    execute: getTags,
    isFetching: tagsFetching
  } = useQuery({
    query: ListTagsByNodeIdsDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables: listTagsByNodeIdsVariables
  })

  // Get metrics for nodes
  const metricsVariables = reactive({
    id: 0,
    instance: '',
    monitor: Monitor.ICMP,
    timeRange: 1,
    timeRangeUnit: TimeRangeUnit.Minute
  })
  const {
    execute: getMetrics,
    isFetching: metricsFetching
  } = useQuery({
    query: NodeLatencyMetricDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables: metricsVariables
  })

  const formatNodes = async (data: Partial<Node>[], monitoredState: MonitoredState) => {
    switch (monitoredState) {
      case MonitoredState.Detected:
        return await formatDetectedNodes(data)
      case MonitoredState.Monitored:
        return await formatMonitoredNodes(data)
      case MonitoredState.Unmonitored:
        return await formatUnmonitoredNodes(data)
      default:
        return []
    }
  }

  // sets the initial monitored node object and then calls for tags and metrics
  const formatMonitoredNodes = async (data: Partial<Node>[]) => {
    if (data.length) {
      const nodes: MonitoredNode[] = mapMonitoredNodes(data)

      const tagResult = await getTagsForData(data)
      addTagsToNodes(tagResult.data?.tagsByNodeIds ?? [], nodes)

      const metricsResults = (await getMetricsForData(data))
        .filter((result) => result.data)
        .map((result) => result.data as NodeLatencyMetricQuery)
      addMetricsToMonitoredNodes(metricsResults, nodes)

      return nodes
    } else {
      return []
    }
  }

  const mapMonitoredNodes = (data: Partial<Node>[]): MonitoredNode[] => {
    return data.map((node) => {
      const { ipAddress: snmpPrimaryIpAddress } = node.ipInterfaces?.filter((x) => x.snmpPrimary)[0] ?? {}
      return {
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
        type: MonitoredState.Monitored
      }
    })
  }

  // may be used to get tags for monitored/unmonitored/detected nodes
  const getTagsForData = async (data: Partial<Node>[]) => {
    listTagsByNodeIdsVariables.nodeIds = data.map((node) => node.id)
    return await getTags()
  }

  // may only be used for monitored nodes
  const getMetricsForData = async (data: Partial<Node>[]) => {
    const promises = []
    for (const node of data) {
      const { ipAddress: snmpPrimaryIpAddress } = node.ipInterfaces?.filter((x) => x.snmpPrimary)[0] ?? {}
      const instance = node.scanType === AZURE_SCAN ? `azure-node-${node.id}` : snmpPrimaryIpAddress!

      metricsVariables.id = node.id
      metricsVariables.instance = instance
      promises.push(getMetrics())
    }

    return await Promise.all(promises)
  }

  // callback after getTags call complete
  const addTagsToNodes = (tags: NodeTags[], nodeList: InventoryNode[]) => {
    return nodeList.map((node) => {
      tags.forEach((tag) => {
        if (node.id === tag.nodeId) {
          node.anchor.tagValue = tag.tags ?? []
        }
      })

      return node
    })
  }

  const addMetricsToMonitoredNodes = (results: NodeLatencyMetricQuery[], monitoredNodes: MonitoredNode[]) => {
    const nodeIdMap: Map<number, Chip[]> = new Map()

    results.forEach((data) => {
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

      nodeIdMap.set(Number(nodeId), [
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
      ])
    })

    return monitoredNodes.map((node) => {
      const metrics = nodeIdMap.get(node.id)
      if (metrics) {
        node.metrics = metrics
      }

      return node
    })
  }

  const formatUnmonitoredNodes = async (data: Partial<Node>[]): Promise<UnmonitoredNode[]> => {
    if (data.length) {
      const tagResult = await getTagsForData(data)

      return data.map(({ id, nodeLabel, location, ipInterfaces }) => {
        const { ipAddress: snmpPrimaryIpAddress } = ipInterfaces?.filter((x) => x.snmpPrimary)[0] ?? {}
        const tagsObj = tagResult.data?.tagsByNodeIds?.filter((item) => item.nodeId === id)[0]
        return {
          id: id,
          label: nodeLabel!,
          anchor: {
            locationValue: location?.location ?? '--',
            tagValue: tagsObj?.tags ?? [],
            managementIpValue: snmpPrimaryIpAddress ?? ''
          },
          isNodeOverlayChecked: false,
          type: MonitoredState.Unmonitored
        }
      })
    } else {
      return []
    }
  }

  const formatDetectedNodes = async (data: Partial<Node>[]): Promise<DetectedNode[]> => {
    if (data.length) {
      const tagResult = await getTagsForData(data)

      return data.map(({ id, nodeLabel, location }) => {
        const tagsObj = tagResult.data?.tagsByNodeIds?.filter((item) => item.nodeId === id)[0]
        return {
          id: id,
          label: nodeLabel!,
          anchor: {
            locationValue: location?.location ?? '--',
            tagValue: tagsObj?.tags ?? []
          },
          isNodeOverlayChecked: false,
          type: MonitoredState.Detected
        }
      })
    } else {
      return []
    }
  }

  const fetchByState = async (stateIn: MonitoredState) => {
    if (stateIn === MonitoredState.Monitored) {
      await getMonitoredNodes()
      selectedState.value = MonitoredState.Monitored
    } else if (stateIn === MonitoredState.Unmonitored) {
      await getUnmonitoredNodes()
      selectedState.value = MonitoredState.Unmonitored
    } else if (stateIn === MonitoredState.Detected) {
      await getDetectedNodes()
      selectedState.value = MonitoredState.Detected
    }
  }

  const fetchByLastState = async () => {
    await fetchByState(selectedState.value)
  }

  const setByState = (stateIn: MonitoredState, nodes: InventoryNode[]) => {
    if (stateIn === MonitoredState.Monitored) {
      monitoredNodes.value = nodes as MonitoredNode[]
    } else if (stateIn === MonitoredState.Unmonitored) {
      unmonitoredNodes.value = nodes as UnmonitoredNode[]
    } else if (stateIn === MonitoredState.Detected) {
      detectedNodes.value = nodes as DetectedNode[]
    }
  }

  return {
    monitoredNodes,
    unmonitoredNodes,
    detectedNodes,
    getMonitoredNodes: () => fetchByState(MonitoredState.Monitored),
    getUnmonitoredNodes: () => fetchByState(MonitoredState.Unmonitored),
    getDetectedNodes: () => fetchByState(MonitoredState.Detected),
    getNodesByLabel,
    getNodesByTags,
    fetchByState,
    fetchByLastState,
    isFetching: monitoredNodesFetching || unmonitoredNodesFetching || detectedNodesFetching || tagsFetching || metricsFetching || filteredNodesByTagsFetching || filteredNodesByLabelFetching
  }
})
