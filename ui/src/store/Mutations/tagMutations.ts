import { defineStore } from 'pinia'

export const useTagMutations = defineStore('tagMutations', () => {
  const editTag = (val: string, toAddTags: boolean) => {
    if (toAddTags) {
      // TODO: gql query to add tags to node
    } else {
      // TODO: gql query to remove tags from node
    }

    // display toaster
  }

  return {
    editTag
  }
})
