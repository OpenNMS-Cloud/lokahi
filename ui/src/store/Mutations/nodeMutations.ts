import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import useSpinner from '@/composables/useSpinner'

import { AddNodeDocument, DeleteNodeDocument } from '@/types/graphql'

const { startSpinner, stopSpinner } = useSpinner()

export const useNodeMutations = defineStore('nodeMutations', () => {
  const { execute: addNode, isFetching, error } = useMutation(AddNodeDocument)

  const { execute: deleteNode, isFetching: isDeletingNode } = useMutation(DeleteNodeDocument)

  watchEffect(() => {
    if (isFetching.value || isDeletingNode.value) {
      startSpinner()
    } else {
      stopSpinner()
    }
  })

  return {
    deleteNode,
    addNode,
    error
  }
})
