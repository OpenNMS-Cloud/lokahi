import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import useSpinner from '@/composables/useSpinner'

import { DeleteMinionDocument } from '@/types/graphql'

const { startSpinner, stopSpinner } = useSpinner()

export const useMinionMutations = defineStore('minionMutations', () => {
  const { execute: deleteMinion, isFetching: isDeletingMinion } = useMutation(DeleteMinionDocument)

  watchEffect(() => {
    isDeletingMinion.value ? startSpinner() : stopSpinner()
  })

  return {
    deleteMinion,
    isDeletingMinion
  }
})
