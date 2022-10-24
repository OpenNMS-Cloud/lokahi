import { defineStore } from 'pinia'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'

export const useNodeStatusStore = defineStore('nodeStatusStore', () => {
  const nodeStatusQueries = useNodeStatusQueries()
  const fetchedData = computed(() => nodeStatusQueries.fetchedData)
  
  const setNodeId = (id: number) => {
    nodeStatusQueries.setNodeId(id)
  }

  return {
    fetchedData,
    setNodeId
  }
})