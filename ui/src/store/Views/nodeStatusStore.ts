import { defineStore } from 'pinia'
import { FLOWS_ENABLED } from '@/constants'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { AZURE_SCAN, DeepPartial } from '@/types'
import { DownloadFormat, DownloadIpInterfacesVariables, Exporter, NodeUpdateInput, RequestCriteriaInput } from '@/types/graphql'
import { useNodeMutations } from '../Mutations/nodeMutations'
import { createAndDownloadBlobFile } from '@/components/utils'

export const useNodeStatusStore = defineStore('nodeStatusStore', () => {
  const nodeStatusQueries = useNodeStatusQueries()
  const mutations = useNodeMutations()
  const fetchedData = computed(() => nodeStatusQueries.fetchedData)
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

  return {
    updateNodeAlias,
    fetchedData,
    setNodeId,
    isAzure: computed(() => fetchedData.value.node.scanType === AZURE_SCAN),
    fetchExporters,
    exporters,
    node,
    nodeId,
    downloadIpInterfacesToCsv
  }
})
