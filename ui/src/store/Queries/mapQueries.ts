import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { NodesForMapDocument } from '@/types/graphql'

export const useMapQueries = defineStore('mapQueries', () => {
  const nodes = ref()

  const { data, execute, isFetching } = useQuery({
    query: NodesForMapDocument,
    cachePolicy: 'network-only' // always fetch and do not cache
  })

  watchEffect(() => {
    nodes.value = data.value?.findAllNodes || []
  })

  return {
    nodes,
    fetch: execute,
    isFetching
  }
})