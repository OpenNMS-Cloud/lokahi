import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import { 
  CreateAzureActiveDiscoveryDocument, 
  CreateIcmpActiveDiscoveryDocument, 
  UpsertPassiveDiscoveryDocument,
  TogglePassiveDiscoveryDocument,
  CreateOrUpdateActiveIcmpDiscoveryDocument,
  DeleteActiveIcmpDiscoveryDocument
} from '@/types/graphql'

export const useDiscoveryMutations = defineStore('discoveryMutations', () => {
  // Create Azure
  const { execute: addAzureCreds, error, isFetching } = useMutation(CreateAzureActiveDiscoveryDocument)

  // Create ICMP Discoveries
  const {
    execute: createDiscoveryConfig,
    error: activeDiscoveryError,
    isFetching: isFetchingActiveDiscovery
  } = useMutation(CreateIcmpActiveDiscoveryDocument)

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
    execute: deleteActiveIcmpDiscovery,
    error: deleteActiveIcmpDiscoveryError,
    isFetching: deleteActiveIcmpDiscoveryIsFetching
  } = useMutation(DeleteActiveIcmpDiscoveryDocument)

  // Toggle Passive Discoveries
  const { execute: togglePassiveDiscovery } = useMutation(TogglePassiveDiscoveryDocument)

  return {
    addAzureCreds,
    azureError: computed(() => error),
    createOrUpdateDiscovery,
    createOrUpdateDiscoveryError,
    createOrUpdateDiscoveryIsFetching,
    deleteActiveIcmpDiscovery,
    deleteActiveIcmpDiscoveryError,
    deleteActiveIcmpDiscoveryIsFetching,
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
