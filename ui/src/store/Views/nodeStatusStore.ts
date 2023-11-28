import { defineStore } from 'pinia'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { useNodeMutations } from '../Mutations/nodeMutations'
import { AZURE_SCAN, DeepPartial } from '@/types'
import { Exporter, NodeUpdateInput, RequestCriteriaInput } from '@/types/graphql'

export const useNodeStatusStore = defineStore('nodeStatusStore', () => {
  const nodeStatusQueries = useNodeStatusQueries()
  const mutations = useNodeMutations()
  const fetchedData = computed(() => nodeStatusQueries.fetchedData)
  const exporters = ref<DeepPartial<Exporter>[]>([])

  const setNodeId = (id: number) => {
    nodeStatusQueries.setNodeId(id)
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
    const data = await nodeStatusQueries.fetchExporters(payload)
    exporters.value = data.value?.findExporters || []
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

  return {
    updateNodeAlias,
    fetchedData,
    setNodeId,
    isAzure: computed(() => fetchedData.value.node.scanType === AZURE_SCAN),
    fetchExporters,
    exporters,
    node
  }
})
