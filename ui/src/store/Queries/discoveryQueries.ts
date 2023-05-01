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
  const tagsSearched = ref([] as Tag[])
  const tagsByDiscovery = ref([] as Tag[] | undefined)
  const discoveryId = ref({
    discoveryId: 0
  })
  const { data: locations, execute: getLocations } = useQuery({
    query: ListLocationsForDiscoveryDocument,
    fetchOnMount: false
  })

  const { data: listedDiscoveries, execute: getDiscoveries } = useQuery({
    query: ListDiscoveriesDocument,
    fetchOnMount: false
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
    return activeDiscoveries.map((discovery) => ({
      ...discovery.details,
      discoveryType: discovery.discoveryType
    }))
  }

  // active discoveries
  const { data: tagsByDiscoveryIdData, execute: tagsByDiscoveryIdExecute } = useQuery({
    query: TagsByActiveDiscoveryIdDocument,
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
    variables: discoveryId
  })

  const getTagsByPassiveDiscoveryId = async (id: number) => {
    discoveryId.value.discoveryId = id
    await tagsByPassiveDiscoveryIdExecute()
    tagsByDiscovery.value = tagsByPassiveDiscoveryIdData.value?.tagsByPassiveDiscoveryId || []
  }

  return {
    locations: computed(() => locations.value?.findAllLocations || []),
    getLocations,
    tagsSearched: computed(() => tagsSearched.value || []),
    getTagsSearch,
    activeDiscoveries: computed(() => formatActiveDiscoveries(listedDiscoveries.value?.listActiveDiscovery) || []),
    passiveDiscoveries: computed(() => listedDiscoveries.value?.passiveDiscoveries || []),
    getDiscoveries,
    getTagsByActiveDiscoveryId,
    tagsByActiveDiscoveryId: computed(() => tagsByDiscovery.value),
    getTagsByPassiveDiscoveryId,
    tagsByPassiveDiscoveryId: computed(() => tagsByDiscovery.value)
  }
})
