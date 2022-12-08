import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { Event, ListNodeStatusDocument, Node } from '@/types/graphql'

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
    events: data.value?.events || [] as Event[],
    node: data.value?.node || {} as Node
  }))

  return {
    setNodeId,
    fetchedData
  }
})
