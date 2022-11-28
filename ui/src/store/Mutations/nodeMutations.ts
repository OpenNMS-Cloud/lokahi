import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import useSpinner from '@/composables/useSpinner'

import { AddNodeDocument } from '@/types/graphql'

const { startSpinner, stopSpinner } = useSpinner()

export const useNodeMutations = defineStore('nodeMutations', () => {
  const {
    execute: addNode,
    isFetching,
    error
  } = useMutation(AddNodeDocument)

  watchEffect(() => {
    if (isFetching.value) {
      startSpinner()
    } else {
      stopSpinner()
    }
  })

  return {
    addNode,
    error
  }
})
