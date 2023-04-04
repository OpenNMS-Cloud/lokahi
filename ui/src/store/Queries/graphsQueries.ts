import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { GetNodeForGraphsDocument, DefaultNode } from '@/types/graphql'

export const useGraphsQueries = defineStore('graphsQueries', () => {
  const variables = ref({})

  const setNodeId = (id: number) => {
    variables.value = { id }
  }

  const { data, execute, isDone } = useQuery({
    query: GetNodeForGraphsDocument,
    cachePolicy: 'network-only',
    variables,
    fetchOnMount: false
  })

  const node = computed(() => (data.value?.node?.details as DefaultNode) || ({} as DefaultNode))

  return {
    node,
    setNodeId,
    fetchNode: execute,
    fetchIsDone: isDone
  }
})
