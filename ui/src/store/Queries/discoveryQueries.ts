import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { ListLocationsForDiscoveryDocument } from '@/types/graphql'

export const useDiscoveryQueries = defineStore('discoveryQueries', () => {
  const { data: locations, execute: getLocations } = useQuery({
    query: ListLocationsForDiscoveryDocument,
    fetchOnMount: false
  })

  return {
    locations: computed(() => locations.value?.findAllLocations || []),
    getLocations,
  }
})
