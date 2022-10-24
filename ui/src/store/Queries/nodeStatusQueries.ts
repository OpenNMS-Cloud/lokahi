import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { ListDeviceStatusDocument } from '@/types/graphql'

export const useNodeStatusQueries = defineStore('nodeStatusQueries', () => {
  const variables = ref({})

  const setNodeId = (id: number) => {
    variables.value = { id }
  }

  const { data } = useQuery({
    query: ListDeviceStatusDocument,
    variables,
    cachePolicy: 'network-only'

  })

  const fetchedData = computed(() => ({
    listEvents: data.value?.listEvents || {},
    device: data.value?.device || {},
    deviceLatency: data.value?.deviceLatency?.data?.result || [],
    deviceUptime: data.value?.deviceUptime?.data?.result || []
  }))

  return {
    setNodeId,
    fetchedData
  }
})