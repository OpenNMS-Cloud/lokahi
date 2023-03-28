import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { ListMonitoryPoliciesDocument } from '@/types/graphql'

export const useMonitoringPoliciesQueries = defineStore('monitoringPoliciesQueries', () => {
  const { data: monitoringPolicies, execute: listMonitoringPolicies } = useQuery({
    query: ListMonitoryPoliciesDocument,
    cachePolicy: 'network-only'
  })

  return {
    monitoringPolicies: computed(() => monitoringPolicies.value?.listMonitoryPolicies || []),
    listMonitoringPolicies
  }
})
