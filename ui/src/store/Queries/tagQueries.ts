import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { ListTagsDocument, Tag, ListTagsSearchDocument } from '@/types/graphql'

const mockTags = [
  { name: 'tag1' },
  { name: 'tag2' },
  { name: 'tag3' },
  { name: 'tag4' },
  { name: 'tag5' },
  { name: 'tag6' },
  { name: 'tag7' },
  { name: 'tag8' },
  { name: 'tag9' },
  { name: 'tag10' },
  { name: 'tag11' },
  { name: 'tag12' },
  { name: 'tag13' },
  { name: 'tag14' },
  { name: 'tag15' },
  { name: 'tag16' },
  { name: 'tag17' },
  { name: 'tag18' },
  { name: 'tag19' },
  { name: 'tag20' },
  { name: 'tag21' },
  { name: 'tag22' },
  { name: 'tag23' },
  { name: 'tag24' },
  { name: 'tag25' },
  { name: 'tag26' },
  { name: 'tag27' }
]

export const useTagQueries = defineStore('tagQueries', () => {
  const tags = ref([])
  const tagsSearched = ref([] as Tag[])

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

  /* const fetchTags = () => {
    const { data, execute } = useQuery({
      query: ListTagsDocument,
      fetchOnMount: false
    })

    execute()

    watchEffect(() => {
      console.log('data', data.value?.tags)
      // tags.value = mockTags
      return data.value?.tags || []
    })
  } */

  const searchTag = (searchTag: string) => {
    // TODO: add gql query
    // tags.value = tags.value.filter((tag) => tag === searchTag)
  }

  const resetTags = () => {
    tags.value = []
  }

  return {
    tagsSearched: computed(() => tagsSearched.value || []),
    getTagsSearch,
    // tags: computed(() => tagsFetched.value?.tags || []),
    // tags,
    // fetchTags,
    searchTag,
    resetTags
  }
})
