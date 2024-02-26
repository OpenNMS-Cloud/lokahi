import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import {
  CreateAzureActiveDiscoveryDocument,
  CreateIcmpActiveDiscoveryDocument,
  UpsertPassiveDiscoveryDocument,
  TogglePassiveDiscoveryDocument,
  CreateOrUpdateActiveIcmpDiscoveryDocument,
  DeleteActiveIcmpDiscoveryDocument,
  DeletePassiveDiscoveryDocument
} from '@/types/graphql'

export const useDiscoveryMutations = defineStore('discoveryMutations', () => {
  // Create Azure
  const { execute: addAzureCreds, error: azureError, isFetching } = useMutation(CreateAzureActiveDiscoveryDocument)

  // Create ICMP Discoveries
  const {
    execute: createDiscoveryConfig,
    error: activeDiscoveryError,
    isFetching: isFetchingActiveDiscovery
  } = useMutation(CreateIcmpActiveDiscoveryDocument)

  // Delete ICMP Discoveries
  const {
    execute: deleteActiveIcmpDiscovery,
    error: deleteActiveIcmpDiscoveryError,
    isFetching: deleteActiveIcmpDiscoveryIsFetching
  } = useMutation(DeleteActiveIcmpDiscoveryDocument)

  // Create Passive Discoveries
  const {
    execute: upsertPassiveDiscovery,
    error: passiveDiscoveryError,
    isFetching: isFetchingPassiveDiscovery
  } = useMutation(UpsertPassiveDiscoveryDocument)

  const {
    execute: createOrUpdateDiscovery,
    error: createOrUpdateDiscoveryError,
    isFetching: createOrUpdateDiscoveryIsFetching
  } = useMutation(CreateOrUpdateActiveIcmpDiscoveryDocument)

  const {
    execute: deletePassiveDiscovery,
    error: deletePassiveDiscoveryError,
    isFetching: deletePassiveDiscoveryIsFetching
  } = useMutation(DeletePassiveDiscoveryDocument)

  // Toggle Passive Discoveries
  const { execute: togglePassiveDiscovery } = useMutation(TogglePassiveDiscoveryDocument)

  return {
    addAzureCreds,
    azureError,
    createOrUpdateDiscovery,
    createOrUpdateDiscoveryError,
    createOrUpdateDiscoveryIsFetching,
    deleteActiveIcmpDiscovery,
    deleteActiveIcmpDiscoveryError,
    deleteActiveIcmpDiscoveryIsFetching,
    deletePassiveDiscovery,
    deletePassiveDiscoveryError,
    deletePassiveDiscoveryIsFetching,
    isFetching: computed(() => isFetching),
    createDiscoveryConfig,
    activeDiscoveryError: computed(() => activeDiscoveryError),
    isFetchingActiveDiscovery: computed(() => isFetchingActiveDiscovery),
    upsertPassiveDiscovery,
    passiveDiscoveryError: computed(() => passiveDiscoveryError.value),
    isFetchingPassiveDiscovery: computed(() => isFetchingPassiveDiscovery.value),
    togglePassiveDiscovery
  }
})
