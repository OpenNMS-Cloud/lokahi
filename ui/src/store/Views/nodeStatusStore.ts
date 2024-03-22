import { defineStore } from 'pinia'
import { FLOWS_ENABLED } from '@/constants'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { AZURE_SCAN, DeepPartial } from '@/types'
import { DownloadFormat, DownloadCsvVariables, Exporter, ListAlertResponse, NodeUpdateInput, RequestCriteriaInput, TimeRange } from '@/types/graphql'
import { useNodeMutations } from '../Mutations/nodeMutations'
import { createAndDownloadBlobFile } from '@/components/utils'
import { AlertsFilters, AlertsSort, Pagination } from '@/types/alerts'
import { cloneDeep } from 'lodash'

const alertsFilterDefault: AlertsFilters = {
  timeRange: TimeRange.All,
  nodeLabel: '',
  severities: [],
  sortAscending: true,
  sortBy: 'id',
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
  const fetchedEventsData = computed(() => nodeStatusQueries.fetchedEventsData)
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

    const azureInterfaces = new Map(node.azureInterfaces?.map((azureInterface: any) => {
      return [azureInterface.id, azureInterface]
    }))

    const snmpInterfaces = node.snmpInterfaces?.map((snmpInterface: any) => {
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
    const downloadTopNQueryVariables: DownloadCsvVariables = {
      nodeId: nodeId.value,
      searchTerm: searchTerm,
      downloadFormat: DownloadFormat.Csv
    }
    const bytes = await nodeStatusQueries.downloadIpInterfaces(downloadTopNQueryVariables)
    createAndDownloadBlobFile(bytes, `${node.value.nodeLabel}-ip-interfaces.csv`)
  }

  const downloadAlertsByNodesToCsv = async () => {
    const page = alertsPagination.value.page > 0 ? alertsPagination.value.page - 1 : 0
    const pagination = {
      ...alertsPagination.value,
      page
    }

    const bytes = await nodeStatusQueries.downloadAlertsByNode(alertsFilter.value, pagination, { downloadFormat: DownloadFormat.Csv })
    const filename = `${node.value.nodeLabel}-recent-alerts.csv`
    createAndDownloadBlobFile(bytes || [], filename)
  }

  const downloadEvents = async (searchTerm: string) => {
    const queryVariables: DownloadCsvVariables = {
      nodeId: nodeId.value,
      searchTerm: searchTerm || '',
      downloadFormat: DownloadFormat.Csv
    }
    const bytes = await nodeStatusQueries.downloadEvents(queryVariables)
    const fileName = `${node.value.nodeLabel}-events.csv`
    createAndDownloadBlobFile(bytes, fileName)
  }

  const getAlertsByNode = async () => {

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

  const setAlertsByNodePage = (page: number): void => {
    if (page !== Number(alertsPagination.value.page)) {
      alertsPagination.value = {
        ...alertsPagination.value,
        page
      }
    }

    getAlertsByNode()
  }

  const setAlertsByNodePageSize = (pageSize: number): void => {
    if (pageSize !== alertsPagination.value.pageSize) {
      alertsPagination.value = {
        ...alertsPagination.value,
        page: 1, // always request first page on change
        pageSize
      }
    }

    getAlertsByNode()
  }

  const alertsByNodeSortChanged = (sortObj: AlertsSort) => {

    alertsFilter.value = {
      ...alertsFilter.value,
      sortBy: sortObj.sortBy,
      sortAscending: sortObj.sortAscending
    }

    if (alertsPagination.value.page !== 1 || alertsPagination.value.total !== fetchAlertsByNodeData.value.totalAlerts) {
      alertsPagination.value = {
        ...alertsPagination.value,
        page: 1, // always request first page on change
        total: fetchAlertsByNodeData.value.totalAlerts
      }
    }

    getAlertsByNode()
  }

  return {
    updateNodeAlias,
    fetchedData,
    fetchedEventsData,
    setNodeId,
    isAzure: computed(() => fetchedData.value.node.scanType === AZURE_SCAN),
    fetchExporters,
    exporters,
    node,
    nodeId,
    downloadIpInterfacesToCsv,
    getAlertsByNode,
    fetchAlertsByNodeData,
    alertsPagination,
    setAlertsByNodePageSize,
    setAlertsByNodePage,
    alertsByNodeSortChanged,
    downloadAlertsByNodesToCsv,
    downloadEvents
  }
})
