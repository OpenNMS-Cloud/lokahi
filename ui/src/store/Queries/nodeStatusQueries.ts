import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { DownloadIpInterfacesDocument, DownloadIpInterfacesVariables, Event, FindExportersForNodeStatusDocument, ListNodeStatusDocument, Node, RequestCriteriaInput } from '@/types/graphql'

export const useNodeStatusQueries = defineStore('nodeStatusQueries', () => {
  const variables = ref({})

  const setNodeId = (id: number) => {
    variables.value = { id }
  }

  const { data, execute: fetchNodeStatus } = useQuery({
    query: ListNodeStatusDocument,
    variables,
    cachePolicy: 'network-only'
  })

  const fetchedData = computed(() => ({
    events: data.value?.events || ([] as Event[]),
    node: data.value?.node || ({} as Node)
  }))

  const fetchExporters = async (requestCriteria: RequestCriteriaInput) => {
    const { execute, data } = useQuery({
      query: FindExportersForNodeStatusDocument,
      variables: {
        requestCriteria
      },
      cachePolicy: 'network-only'
    })
    await execute()
    return data
  }

  const downloadIpInterfaces = async (requestCriteria: DownloadIpInterfacesVariables) => {
    const { execute, data } = useQuery({
      query: DownloadIpInterfacesDocument,
      variables: requestCriteria,
      cachePolicy: 'network-only',
      fetchOnMount: false
    })
    await execute()
    return data.value?.downloadIpInterfacesByNodeAndSearchTerm?.ipInterfaces
  }

  return {
    setNodeId,
    fetchedData,
    fetchExporters,
    fetchNodeStatus,
    downloadIpInterfaces
  }
})
