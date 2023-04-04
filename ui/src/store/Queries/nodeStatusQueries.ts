import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { Event, ListNodeStatusDocument, DefaultNode } from '@/types/graphql'

export const useNodeStatusQueries = defineStore('nodeStatusQueries', () => {
  const variables = ref({})

  const setNodeId = (id: number) => {
    variables.value = { id }
  }

  const { data } = useQuery({
    query: ListNodeStatusDocument,
    variables,
    cachePolicy: 'network-only'
  })

  const fetchedData = computed(() => ({
    events: data.value?.events || ([] as Event[]),
    node: (data.value?.node?.details as DefaultNode) || ({} as DefaultNode)
  }))

  return {
    setNodeId,
    fetchedData
  }
})
