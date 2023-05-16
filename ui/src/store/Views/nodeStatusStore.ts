import { defineStore } from 'pinia'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { AZURE_SCAN } from '@/types'

export const useNodeStatusStore = defineStore('nodeStatusStore', () => {
  const nodeStatusQueries = useNodeStatusQueries()
  const fetchedData = computed(() => nodeStatusQueries.fetchedData)

  const setNodeId = (id: number) => {
    nodeStatusQueries.setNodeId(id)
  }

  return {
    fetchedData,
    setNodeId,
    isAzure: computed(() => fetchedData.value.node.scanType === AZURE_SCAN)
  }
})
