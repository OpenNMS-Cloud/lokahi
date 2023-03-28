import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import { CreateMonitorPolicyDocument } from '@/types/graphql'

export const useMonitoringPoliciesMutations = defineStore('monitoringPoliciesMutations', () => {
  const { execute: addMonitoringPolicy, error, isFetching } = useMutation(CreateMonitorPolicyDocument)

  return {
    addMonitoringPolicy,
    error: computed(() => error),
    isFetching: computed(() => isFetching)
  }
})
