import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import { CreateMonitorPolicyDocument, DeletePolicyByIdDocument, DeleteRuleByIdDocument } from '@/types/graphql'

export const useMonitoringPoliciesMutations = defineStore('monitoringPoliciesMutations', () => {
  const { execute: addMonitoringPolicy, error, isFetching } = useMutation(CreateMonitorPolicyDocument)
  
  const { execute: deleteMonitoringPolicy, isFetching: deleteIsFetching } = useMutation(DeletePolicyByIdDocument) 

  const { execute: deleteRule, isFetching: deleteRuleIsFetching } = useMutation(DeleteRuleByIdDocument) 

  return {
    addMonitoringPolicy,
    error: computed(() => error),
    isFetching: computed(() => isFetching),
    deleteMonitoringPolicy,
    deleteIsFetching,
    deleteRule,
    deleteRuleIsFetching
  }
})
