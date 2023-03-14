import { IPolicy } from '@/types/policies'
import { defineStore } from 'pinia'
import { useMutation } from 'villus'
// import { AddMonitoringPolicyDocument } from '@/types/graphql'

export const useMonitoringPoliciesMutations = defineStore('monitoringPoliciesMutations', () => {
  // Add Monitoring Policy
  // const { execute: addMonitoringPolicy, error, isFetching } = useMutation(AddMonitoringPolicyDocument)

  return {
    addMonitoringPolicy: (payload: { policy: IPolicy }) => {
      console.log(payload)
    },
    error: computed(() => ref(false)),
    isFetching: computed(() => ref(false))
  }
})
