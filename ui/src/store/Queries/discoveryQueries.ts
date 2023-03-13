import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import {
  ListLocationsForDiscoveryDocument,
  Tag,
  ListTagsSearchDocument,
  ListDiscoveriesDocument,
  ActiveDiscovery,
  IcmpActiveDiscovery,
  AzureActiveDiscovery
} from '@/types/graphql'
import { DiscoveryType } from '@/components/Discovery/discovery.constants'

export const useDiscoveryQueries = defineStore('discoveryQueries', () => {
  const tagsSearched = ref([] as Tag[])

  const { data: locations, execute: getLocations } = useQuery({
    query: ListLocationsForDiscoveryDocument,
    fetchOnMount: false
  })

  const { data: listedDiscoveries, execute: getDiscoveries } = useQuery({
    query: ListDiscoveriesDocument
  })

  const getTagsSearch = (searchTerm: string) => {
    const { data, error } = useQuery({
      query: ListTagsSearchDocument,
      variables: {
        searchTerm
      }
    })

    watchEffect(() => {
      if (data.value?.tags) {
        tagsSearched.value = data.value.tags
      } else {
        // TODO: what kind of errors and how to manage them
      }
    })
  }

  const formatActiveDiscoveries = (activeDiscoveries: ActiveDiscovery[] = []) => {
    const icmpDiscoveries: IcmpActiveDiscovery[] = []
    const azureDiscoveries: AzureActiveDiscovery[] = []

    for (const discovery of activeDiscoveries) {
      discovery.details.discoveryType = discovery.discoveryType
      if (discovery.discoveryType === DiscoveryType.ICMP) {
        icmpDiscoveries.push(discovery.details)
      }
      if (discovery.discoveryType === DiscoveryType.Azure) {
        azureDiscoveries.push(discovery.details)
      }
    }

    return [...icmpDiscoveries, ...azureDiscoveries]
  }

  return {
    locations: computed(() => locations.value?.findAllLocations || []),
    getLocations,
    tagsSearched: computed(() => tagsSearched.value || []),
    getTagsSearch,
    activeDiscoveries: computed(() => formatActiveDiscoveries(listedDiscoveries.value?.listActiveDiscovery) || []),
    passiveDiscoveries: computed(() => listedDiscoveries.value?.passiveDiscoveries || []),
    getDiscoveries
  }
})
