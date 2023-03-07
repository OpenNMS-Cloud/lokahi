import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import { AddAzureCredentialDocument, CreateActiveDiscoveryDocument, UpsertPassiveDiscoveryDocument } from '@/types/graphql'

export const useDiscoveryMutations = defineStore('discoveryMutations', () => {
  // Create Azure
  const { execute: addAzureCreds, error, isFetching } = useMutation(AddAzureCredentialDocument)

  // Create Active Discoveries
  const {
    execute: createDiscoveryConfig,
    error: activeDiscoveryError,
    isFetching: isFetchingActiveDiscovery
  } = useMutation(CreateActiveDiscoveryDocument)

  // Create Passive Discoveries
  const {
    execute: upsertPassiveDiscovery,
    error: passiveDiscoveryError,
    isFetching: isFetchingPassiveDiscovery
  } = useMutation(UpsertPassiveDiscoveryDocument)

  return {
    addAzureCreds,
    azureError: computed(() => error),
    isFetching: computed(() => isFetching),
    createDiscoveryConfig,
    activeDiscoveryError: computed(() => activeDiscoveryError.value),
    isFetchingActiveDiscovery: computed(() => isFetchingActiveDiscovery.value),
    upsertPassiveDiscovery,
    passiveDiscoveryError: computed(() => passiveDiscoveryError.value),
    isFetchingPassiveDiscovery: computed(() => isFetchingPassiveDiscovery.value),
  }
})
