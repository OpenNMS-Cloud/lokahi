import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import {
  ListLocationsForDiscoveryDocument,
  Tag,
  ListTagsSearchDocument,
  ListDiscoveriesDocument,
  ActiveDiscovery,
  TagsByActiveDiscoveryIdDocument,
  TagsByPassiveDiscoveryIdDocument
} from '@/types/graphql'

export const useDiscoveryQueries = defineStore('discoveryQueries', () => {
  const tagsByDiscovery = ref([] as Tag[])
  const searchTerm = ref('')
  const discoveryId = ref({
    discoveryId: 0
  })
  const { data: locations, execute: getLocations } = useQuery({
    query: ListLocationsForDiscoveryDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false
  })

  const { data: listedDiscoveries, execute: getDiscoveries } = useQuery({
    query: ListDiscoveriesDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false
  })

  const { data:tagsSearched, isFetching:isTagsSearchFetching } = useQuery({
    query: ListTagsSearchDocument,
    cachePolicy: 'network-only',
    variables: {
      searchTerm:searchTerm.value
    }
  })

  const getTagsSearch = async (searchTermIn: string) => {
   
    searchTerm.value = searchTermIn
    
  }

  const formatActiveDiscoveries = (activeDiscoveries: ActiveDiscovery[] = []) => {
    return activeDiscoveries.map((discovery) => ({
      ...discovery.details,
      discoveryType: discovery.discoveryType
    }))
  }

  // active discoveries
  const { data: tagsByDiscoveryIdData, execute: tagsByDiscoveryIdExecute } = useQuery({
    query: TagsByActiveDiscoveryIdDocument,
    cachePolicy: 'network-only',
    variables: discoveryId
  })

  const getTagsByActiveDiscoveryId = async (id: number) => {
    discoveryId.value.discoveryId = id
    await tagsByDiscoveryIdExecute()
    tagsByDiscovery.value = tagsByDiscoveryIdData.value?.tagsByActiveDiscoveryId || []
  }

  // passive discoveries
  const { data: tagsByPassiveDiscoveryIdData, execute: tagsByPassiveDiscoveryIdExecute } = useQuery({
    query: TagsByPassiveDiscoveryIdDocument,
    cachePolicy: 'network-only',
    variables: discoveryId
  })

  const getTagsByPassiveDiscoveryId = async (id: number) => {
    discoveryId.value.discoveryId = id
    await tagsByPassiveDiscoveryIdExecute()
    tagsByDiscovery.value = tagsByPassiveDiscoveryIdData.value?.tagsByPassiveDiscoveryId || []
  }

  return {
    locations: computed(() => locations.value?.findAllLocations ?? []),
    getLocations,
    tagsSearched: computed(() => tagsSearched.value?.tags || []),
    getTagsSearch,
    isTagsSearchFetching,
    activeDiscoveries: computed(() => formatActiveDiscoveries(listedDiscoveries.value?.listActiveDiscovery) || []),
    passiveDiscoveries: computed(() => listedDiscoveries.value?.passiveDiscoveries || []),
    getDiscoveries,
    getTagsByActiveDiscoveryId,
    tagsByActiveDiscoveryId: computed(() => tagsByDiscovery.value),
    getTagsByPassiveDiscoveryId,
    tagsByPassiveDiscoveryId: computed(() => tagsByDiscovery.value)
  }
})
