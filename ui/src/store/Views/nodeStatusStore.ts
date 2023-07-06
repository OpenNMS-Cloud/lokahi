import { defineStore } from 'pinia'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { AZURE_SCAN, DeepPartial } from '@/types'
import { Exporter, RequestCriteriaInput } from '@/types/graphql'

export const useNodeStatusStore = defineStore('nodeStatusStore', () => {
  const nodeStatusQueries = useNodeStatusQueries()
  const fetchedData = computed(() => nodeStatusQueries.fetchedData)
  const exporters = ref<DeepPartial<Exporter>[]>([])

  const setNodeId = (id: number) => {
    nodeStatusQueries.setNodeId(id)
  }

  const fetchExporters = async (id: number) => {
    const payload: RequestCriteriaInput = {
      exporter: [{
        nodeId: id
      }]
    }
    const data = await nodeStatusQueries.fetchExporters(payload)
    exporters.value = data.value?.findExporters || []
  }

  const node = computed(() => {
    const node = nodeStatusQueries.fetchedData.node

    const snmpInterfaces = node.snmpInterfaces?.map((snmpInterface) => {
      for (const exporter of exporters.value) {
        if (exporter.snmpInterface?.ifIndex === snmpInterface.ifIndex) {
          return { ...snmpInterface, exporter }
        }
      }
      return { ...snmpInterface, exporter: {} }
    })

    return { ...node, snmpInterfaces }
  })

  return {
    fetchedData,
    setNodeId,
    isAzure: computed(() => fetchedData.value.node.scanType === AZURE_SCAN),
    fetchExporters,
    exporters,
    node
  }
})
