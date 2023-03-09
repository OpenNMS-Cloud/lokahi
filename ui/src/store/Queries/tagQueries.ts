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
  const fetchTags = async () => {
    await tagExecute()

    if (!tagIsFetching.value) {
      if (!tagError.value) {
        tagStore.setTags(tagData.value?.tags || [])
      } else {
        // TODO: what kind of errors and how to manage them
      }
    }
  }

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

    if (!tagsSearchIsFetching.value) {
      if (!tagsSearchError.value) {
        tagsSearched.value = tagsSearchData.value?.tags || []
      } else {
        // TODO: what kind of errors and how to manage them
      }
    }
  }

  return {
    fetchTags,
    tagsSearched: computed(() => tagsSearched.value || []),
    getTagsSearch
  }
})
