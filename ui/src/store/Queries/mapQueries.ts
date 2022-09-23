import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { DeviceForMapDocument } from '@/types/graphql'

export const useMapQueries = defineStore('mapQueries', () => {
  const devicesAreFetched = ref()
  const fetchedDevices = ref()

  const { data } = useQuery({
    query: DeviceForMapDocument
  })

  watchEffect(() => {
    const devices = data.value?.listDevices?.devices
    devicesAreFetched.value = devices ? true : false
    fetchedDevices.value = devices || []
  })

  return {
    devicesAreFetched,
    fetchedDevices
  }
})