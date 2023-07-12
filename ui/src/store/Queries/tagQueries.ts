import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { ListTagsDocument, Tag, ListTagsSearchDocument } from '@/types/graphql'
import { useTagStore } from '@/store/Components/tagStore'

export const useTagQueries = defineStore('tagQueries', () => {
  const tagsSearched = ref([] as Tag[])
  const tagsSearchTerm = ref({
    searchTerm: ''
  })

  const tagStore = useTagStore()

  const {
    data: tagData,
    execute: tagExecute,
    isFetching: tagIsFetching,
    error: tagError
  } = useQuery({
    query: ListTagsDocument,
    fetchOnMount: false,
    cachePolicy: 'network-only'
  })


  const {
    data: tagsSearchData,
    execute: tagsSearchExecute,
    isFetching: tagsSearchIsFetching,
    error: tagsSearchError
  } = useQuery({
    query: ListTagsSearchDocument,
    variables: tagsSearchTerm,
    fetchOnMount: false,
    cachePolicy: 'network-only'
  })
  const getTagsSearch = async (searchTerm: string) => {
    tagsSearchTerm.value.searchTerm = searchTerm

    await tagsSearchExecute()

    if (tagsSearchData.value?.tags) {
      tagsSearched.value = tagsSearchData?.value.tags
    }
  }

  return {
    tagsSearched: computed(() => tagsSearched.value || []),
    tagsSearchIsFetching,
    tagsSearchExecute,
    getTagsSearch
  }
})
