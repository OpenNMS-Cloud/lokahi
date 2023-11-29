import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import useSpinner from '@/composables/useSpinner'
import {
  AddNodeDocument,
  DeleteNodeDocument,
  AddTagsToNodesDocument,
  TagListNodesAddInput,
  RemoveTagsFromNodesDocument,
  TagListNodesRemoveInput,
  UpdateNodeDocument
} from '@/types/graphql'

const { startSpinner, stopSpinner } = useSpinner()

export const useNodeMutations = defineStore('nodeMutations', () => {
  const { execute: addNode, isFetching, error } = useMutation(AddNodeDocument)

  const { execute: deleteNode, isFetching: isDeletingNode } = useMutation(DeleteNodeDocument)

  const addTagsToNodes = async (tags: TagListNodesAddInput) => {
    const { execute } = useMutation(AddTagsToNodesDocument)

    return await execute({ tags })
  }

  const removeTagsFromNodes = async (tags: TagListNodesRemoveInput) => {
    const { execute } = useMutation(RemoveTagsFromNodesDocument)

    return await execute({ tags })
  }

  watchEffect(() => {
    if (isFetching.value || isDeletingNode.value) {
      startSpinner()
    } else {
      stopSpinner()
    }
  })

  const { execute: updateNode } = useMutation(UpdateNodeDocument)

  return {
    updateNode,
    deleteNode,
    addNode,
    error,
    addTagsToNodes,
    removeTagsFromNodes
  }
})
