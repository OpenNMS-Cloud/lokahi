import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import { CreateMonitorPolicyDocument, DeletePolicyByIdDocument } from '@/types/graphql'

export const useMonitoringPoliciesMutations = defineStore('monitoringPoliciesMutations', () => {
  const { execute: addMonitoringPolicy, error, isFetching } = useMutation(CreateMonitorPolicyDocument)
  
  const { execute: deleteMonitoringPolicy, error: deleteErr, isFetching: deleteIsFetching } = useMutation(DeletePolicyByIdDocument) 

  return {
    addMonitoringPolicy,
    error: computed(() => error),
    isFetching: computed(() => isFetching),
    deleteMonitoringPolicy,
    deleteIsFetching,
    deleteErr
  }
})
