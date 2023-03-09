import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import { ListLocationsDocument, Location } from '@/types/graphql'

export const useLocationsQueries = defineStore('locationsQueries', () => {
  const locations = ref<Location[]>([])

  const { data, execute } = useQuery({
    query: ListLocationsDocument,
    fetchOnMount: false,
    cachePolicy: 'network-only'
  })

  watchEffect(() => {
    locations.value = data.value?.findAllLocations || []
  })

  return {
    fetchLocations: execute,
    locations: computed(() => locations.value)
  }
})
