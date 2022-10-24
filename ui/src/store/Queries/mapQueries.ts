import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { DeviceForMapDocument } from '@/types/graphql'

export const useMapQueries = defineStore('mapQueries', () => {
  const devices = ref()

  const { data, execute, isFetching } = useQuery({
    query: DeviceForMapDocument,
    cachePolicy: 'network-only' // always fetch and do not cache
  })

  watchEffect(() => {
    devices.value = data.value?.listDevices?.devices || []
  })

  return {
    devices,
    fetch: execute,
    isFetching
  }
})