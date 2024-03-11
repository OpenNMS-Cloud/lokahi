import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import {  AlertsByNode, Event, FindExportersForNodeStatusDocument, ListAlertResponse, ListNodeStatusDocument, Node, RequestCriteriaInput } from '@/types/graphql'
import { AlertsFilters, Pagination, Variables } from '@/types/alerts'
import { defaultListAlertResponse } from './alertsQueries'

export const useNodeStatusQueries = defineStore('nodeStatusQueries', () => {
  const variables = ref<Variables>({})
  const fetchAlertsByNodeData = ref({} as ListAlertResponse)
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

  const getAlertsByNodeQuery = async (sortFilter: AlertsFilters, paginationFilter: Pagination) => {
    const { data, execute } = useQuery({
      query: AlertsByNode,
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

    if (data?.value?.getAlertsByNode) {
      fetchAlertsByNodeData.value = {...data?.value?.getAlertsByNode } as ListAlertResponse
    } else {
      fetchAlertsByNodeData.value = defaultListAlertResponse()
    }
  }

  return {
    setNodeId,
    fetchedData,
    fetchExporters,
    fetchNodeStatus,
    getAlertsByNodeQuery,
    fetchAlertsByNodeData
  }
})
