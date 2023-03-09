import { defineStore } from 'pinia'
import { Tag } from '@/types/graphql'

export const useTagStore = defineStore('tagStore', () => {
  const tags = ref([] as Tag[])
  const tagsSelected = ref([] as Tag[])
  const isTagEditMode = ref(false)

  const setTags = (tagList: Tag[]) => {
    tags.value = tagList
  }

  const addNewTag = (newTag: Record<string, string>) => {
    const tagExists = tags.value.some(({ name }) => name === newTag.name)
    if (!tagExists) tags.value.push(newTag as Tag)

    const tagSelectedExists = tagsSelected.value.some(({ name }) => name === newTag.name)
    if (!tagSelectedExists) tagsSelected.value.push(newTag as Tag)
  }

  const setTagEditMode = (isEdit: boolean) => {
    isTagEditMode.value = isEdit
  }

  const toggleTagsSelected = (tag: Tag) => {
    const isTagAlreadySelected = tagsSelected.value.some(({ name }) => name === tag.name)

    if (isTagAlreadySelected) {
      tagsSelected.value = tagsSelected.value.filter(({ name }) => name !== tag.name)
    } else {
      tagsSelected.value.push(tag)
    }
  }

  const selectAllTags = (selectAll: boolean) => {
    tagsSelected.value = selectAll ? tags.value : []
  }

  return {
    tags,
    setTags,
    addNewTag,
    isTagEditMode: computed(() => isTagEditMode.value),
    setTagEditMode,
    selectAllTags,
    tagsSelected: computed(() => tagsSelected.value),
    toggleTagsSelected
  }
})
