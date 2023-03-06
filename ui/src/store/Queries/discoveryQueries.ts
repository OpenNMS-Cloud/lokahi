import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { ListLocationsForDiscoveryDocument, ListDiscoveryConfigDocument } from '@/types/graphql'

export const useDiscoveryQueries = defineStore('discoveryQueries', () => {
  const { data: locations, execute: getLocations } = useQuery({
    query: ListLocationsForDiscoveryDocument,
    fetchOnMount: false
  })

  const { data: listDiscoveryConfig, execute: getDiscoveries } = useQuery({
    query: ListDiscoveryConfigDocument,
    fetchOnMount: false
  })

  return {
    locations: computed(() => locations.value?.findAllLocations || []),
    getLocations,
    discoveries: computed(() => listDiscoveryConfig.value?.listDiscoveryConfig || []),
    getDiscoveries
  }
})
