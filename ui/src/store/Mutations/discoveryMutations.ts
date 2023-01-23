import { defineStore } from 'pinia'
import { useMutation } from 'villus'

import { AddAzureCredentialDocument } from '@/types/graphql'

export const useDiscoveryMutations = defineStore('discoveryMutations', () => {
  const {
    execute: addAzureCreds,
    error,
    isFetching
  } = useMutation(AddAzureCredentialDocument)

  return {
    addAzureCreds,
    azureError: computed(() => error),
    isFetching: computed(() => isFetching)
  }
})
