import { defineStore } from 'pinia'
import { Tag } from '@/types/graphql'
import { useInventoryStore } from '../Views/inventoryStore'
import { useNodeMutations } from '../Mutations/nodeMutations'
import { useInventoryQueries } from '../Queries/inventoryQueries'

export const useTagStore = defineStore('tagStore', () => {
  const tagSelected = ref<Tag>();
  const tagsSelected = ref([] as Tag[])
  const isTagEditMode = ref(false)
  const areAllTagsSelected = ref(false)

  const setTags = (tagList: Tag[]) => {
    tagsSelected.value = [...tagList]
    updateTagEditMode();
  }

  const addNewTag = (newTag: Record<string, string>) => {
    const tagSelectedExists = tagsSelected.value.some(({ name }) => name === newTag.name)
    if (!tagSelectedExists) tagsSelected.value.push(newTag as Tag)
    updateTagEditMode();
  }
  const updateTagEditMode = () => {
    isTagEditMode.value = tagsSelected.value.length > 0;
  }
  const setTagEditMode = (isEdit: boolean) => {
    isTagEditMode.value = isEdit
  }

  const deactivateTag = (tagIndex: number) => {
    const newTags = [...tagsSelected.value];
    newTags.splice(tagIndex, 1);
    setTags(newTags);
    if (newTags.length === 0) {
      setTagEditMode(false)
      const inventoryStore = useInventoryStore();
      inventoryStore.nodesSelected = [];
    }
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
    tagsSelected.value = selectAll ? tagsSelected.value : []
  }


  const resetState = () => {
    areAllTagsSelected.value = false
    tagsSelected.value = [];
  }

  const saveTagsToSelectedNodes = async () => {
    const tags = tagsSelected.value.map(({ name }) => ({ name }))
    const inventoryStore = useInventoryStore();
    const nodeMutations = useNodeMutations();
    const inventoryQueries = useInventoryQueries();
    const nodeIds = inventoryStore.nodesSelected.map((node) => node.id)
    await nodeMutations.addTagsToNodes({ nodeIds, tags })
    await inventoryQueries.fetchByLastState();
    tagsSelected.value = []
    inventoryStore.isTagManagerOpen = false;
    inventoryStore.nodesSelected = [];
    isTagEditMode.value = false;
    resetState()
  }

  const toggleSelectAll = () => {
    areAllTagsSelected.value = !areAllTagsSelected.value
    selectAllTags(areAllTagsSelected.value)
  }

  return {
    tagSelected,
    tagsSelected,
    areAllTagsSelected,
    setTags,
    addNewTag,
    isTagEditMode: computed(() => isTagEditMode.value),
    saveTagsToSelectedNodes,
    setTagEditMode,
    selectAllTags,
    toggleSelectAll,
    toggleTagsSelected,
    deactivateTag,
    updateTagEditMode
  }
})
