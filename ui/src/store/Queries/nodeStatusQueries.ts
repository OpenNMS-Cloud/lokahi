import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { AlertsByNodeDocument, DownloadIpInterfacesDocument, DownloadIpInterfacesVariables, Event, FindExportersForNodeStatusDocument, ListAlertResponse, ListNodeEventsDocument, ListNodeStatusDocument, Node, RequestCriteriaInput } from '@/types/graphql'
import { AlertsFilters, Pagination, Variables } from '@/types/alerts'
import { defaultListAlertResponse } from './alertsQueries'

export const useNodeStatusQueries = defineStore('nodeStatusQueries', () => {
  const variables = ref<Variables>({})
  const fetchAlertsByNodeData = ref({} as ListAlertResponse)
  const setNodeId = (id: number) => {
    variables.value = { id }
  }

  const { data, execute: fetchNodeStatus } = useQuery({
    query: ListNodeStatusDocument,
    variables,
    cachePolicy: 'network-only'
  })

  const { data: events, execute: fetchEvents } = useQuery({
    query: ListNodeEventsDocument,
    variables,
    cachePolicy: 'network-only'
  })

  const fetchedData = computed(() => ({
    node: data.value?.node || ({} as Node)
  }))

  const fetchedEventsData = computed(() => ({
    events: events.value?.events || ([] as Event[])
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
      query: AlertsByNodeDocument,
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
    fetchedEventsData,
    fetchExporters,
    fetchNodeStatus,
    fetchEvents,
    downloadIpInterfaces,
    getAlertsByNodeQuery,
    fetchAlertsByNodeData
  }
})
