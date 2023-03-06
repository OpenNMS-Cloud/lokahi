import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { ListTagsDocument, Tag } from '@/types/graphql'
import { TagNodesType } from '@/types/tags'

export const useTagStore = defineStore('tagStore', () => {
  const tags = ref([] as Tag[])
  const selectedTags = ref([] as Tag[])
  const tagNodesSelected = ref(TagNodesType.Unselected)

  /* const { data: fetchedTags, execute: fetchTags } = useQuery({
    query: ListTagsDocument,
    fetchOnMount: false
  }) */
  const fetchTags = () => {
    const { data, execute } = useQuery({
      query: ListTagsDocument,
      fetchOnMount: false,
      cachePolicy: 'network-only'
    })

    execute()

    watchEffect(() => {
      console.log('data', data.value?.tags)
      // tags.value = mockTags
      // return data.value?.tags || []
      tags.value = data.value?.tags || []
    })
  }

  const resetTags = () => {
    tags.value = []
  }

  const updateSelectedTags = (tags) => {
    selectedTags.value = tags
  }

  const toggleTag = (tag) => {
    if (selectedTags.value.includes(tag)) {
      selectedTags.value = selectedTags.value.filter((t) => t !== tag)
    } else {
      selectedTags.value.push(tag)
    }
  }

  const selectAllTags = (selectAll: boolean) => {
    console.log('selectAllTags', selectAll)
    selectedTags.value = selectAll ? tags.value : []
    console.log('selectAllTags', selectedTags.value)
  }

  const selectTagNodes = (type: TagNodesType) => {
    tagNodesSelected.value = type
  }

  return {
    fetchTags,
    tags, //: fetchedTags.value?.tags || [],
    resetTags,
    selectAllTags,
    selectedTags,
    tagNodesSelected,
    updateSelectedTags,
    toggleTag,
    selectTagNodes
  }
})
