import { defineStore } from 'pinia'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'

export const useNodeStore = defineStore('nodeStore', () => {
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