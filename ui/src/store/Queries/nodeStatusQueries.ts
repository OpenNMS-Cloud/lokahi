import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import {  AlertsNodeByDocument, Event, FindExportersForNodeStatusDocument, ListNodeStatusDocument, Node, RequestCriteriaInput } from '@/types/graphql'
import { AlertsFilters, Pagination, Variables } from '@/types/alerts'

export const useNodeStatusQueries = defineStore('nodeStatusQueries', () => {
  const variables = ref<Variables>({})
  const fetchNodeByAlertData = ref()

  const setNodeId = (id: number) => {
    variables.value.id = id
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

  const getNodeByAlertsQuery = async (sortFilter: AlertsFilters, paginationFilter: Pagination) => {
    const { data, execute } = useQuery({
      query: AlertsNodeByDocument,
      variables: {
        page: paginationFilter.page,
        pageSize: paginationFilter.pageSize,
        sortBy: sortFilter.sortBy,
        sortAscending: sortFilter.sortAscending,
        nodeId: variables.value.id
      },
      cachePolicy: 'network-only'
    })
    await execute()
    fetchNodeByAlertData.value = data?.value?.getAlertsByNode?.alerts || []
  }

  return {
    setNodeId,
    fetchedData,
    fetchExporters,
    fetchNodeStatus,
    getNodeByAlertsQuery,
    fetchNodeByAlertData
  }
})
