import { defineStore } from 'pinia'
import { FLOWS_ENABLED } from '@/constants'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { AZURE_SCAN, DeepPartial } from '@/types'
import { DownloadFormat, DownloadIpInterfacesVariables, Exporter, ListAlertResponse, NodeUpdateInput, RequestCriteriaInput, TimeRange } from '@/types/graphql'
import { useNodeMutations } from '../Mutations/nodeMutations'
import { createAndDownloadBlobFile } from '@/components/utils'
import { AlertsFilters, Pagination } from '@/types/alerts'
import { cloneDeep } from 'lodash'

const alertsFilterDefault: AlertsFilters = {
  timeRange: TimeRange.All,
  nodeLabel: '',
  severities: [],
  sortAscending: false,
  sortBy: 'lastEventTime',
  nodeId: 1
}

const alertsPaginationDefault: Pagination = {
  page: 0, // FE pagination component has base 1 (first page)
  pageSize: 10,
  total: 0
}

export const useNodeStatusStore = defineStore('nodeStatusStore', () => {
  const alertsFilter = ref(cloneDeep(alertsFilterDefault))
  const alertsPagination = ref(cloneDeep(alertsPaginationDefault))
  const nodeStatusQueries = useNodeStatusQueries()
  const mutations = useNodeMutations()
  const fetchedData = computed(() => nodeStatusQueries.fetchedData)
  const fetchAlertsByNodeData = ref({} as ListAlertResponse)
  const exporters = ref<DeepPartial<Exporter>[]>([])
  const nodeId = ref()

  const setNodeId = (id: number) => {
    nodeStatusQueries.setNodeId(id)
    nodeId.value = id
  }

  const fetchExporters = async (id: number) => {
    // flows can be queried up to last 7 days.
    const now = new Date()
    const startTime = now.setDate(now.getDate() - 7)
    const endTime = Date.now()

    const payload: RequestCriteriaInput = {
      exporter: [{
        nodeId: id
      }],
      timeRange: {
        startTime,
        endTime
      }
    }

    if (FLOWS_ENABLED) {
      const data = await nodeStatusQueries.fetchExporters(payload)
      exporters.value = data.value?.findExporters || []
    } else {
      exporters.value = []
    }
  }

  // add the exporter object to the matching node snmp interface (match on ifIndex)
  // which is used to show if the interface is observing flows data
  const node = computed(() => {
    const node = nodeStatusQueries.fetchedData.node

    const azureInterfaces = new Map(node.azureInterfaces?.map((azureInterface) => {
      return [azureInterface.id, azureInterface]
    }))

    const snmpInterfaces = node.snmpInterfaces?.map((snmpInterface) => {
      for (const exporter of exporters.value) {
        if (exporter.snmpInterface?.ifIndex === snmpInterface.ifIndex) {
          return { ...snmpInterface, exporter }
        }
      }
      return { ...snmpInterface, exporter: {} }
    }) || []

    return { ...node, snmpInterfaces, azureInterfaces }
  })

  const updateNodeAlias = async (nodeAlias: string) => {
    const updateInput: NodeUpdateInput = {
      id: node.value.id,
      nodeAlias
    }

    await mutations.updateNode({ node: updateInput })
  }

  const downloadIpInterfacesToCsv = async (searchTerm: string) => {
    const downloadTopNQueryVariables: DownloadIpInterfacesVariables = {
      nodeId: nodeId.value,
      searchTerm: searchTerm,
      downloadFormat: DownloadFormat.Csv
    }
    const bytes = await nodeStatusQueries.downloadIpInterfaces(downloadTopNQueryVariables)
    createAndDownloadBlobFile(bytes, `${node.value.nodeLabel}-ip-interfaces.csv`)
  }

  const getNodeByAlerts = async () => {

    const page = alertsPagination.value.page > 0 ? alertsPagination.value.page - 1 : 0

    const pagination = {
      ...alertsPagination.value,
      page
    }
    await nodeStatusQueries.getAlertsByNodeQuery(alertsFilter.value, pagination)

    fetchAlertsByNodeData.value = nodeStatusQueries.fetchAlertsByNodeData

    if (fetchAlertsByNodeData.value.totalAlerts != alertsPagination.value.total) {
      alertsPagination.value = {
        ...alertsPagination.value,
        total: fetchAlertsByNodeData.value.totalAlerts
      }
    }
  }

  return {
    updateNodeAlias,
    fetchedData,
    setNodeId,
    isAzure: computed(() => fetchedData.value.node.scanType === AZURE_SCAN),
    fetchExporters,
    exporters,
    node,
    nodeId,
    downloadIpInterfacesToCsv,
    getNodeByAlerts,
    fetchAlertsByNodeData,
    alertsPagination
  }
})
