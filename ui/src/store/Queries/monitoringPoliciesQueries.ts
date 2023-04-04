import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { ListMonitoryPoliciesDocument } from '@/types/graphql'

export const useMonitoringPoliciesQueries = defineStore('monitoringPoliciesQueries', () => {
  const { data, execute: listMonitoringPolicies } = useQuery({
    query: ListMonitoryPoliciesDocument,
    cachePolicy: 'network-only'
  })

  const monitoringPolicies = computed(() => {
    if (!data.value) return []

    const policies = data.value?.listMonitoryPolicies || []

    if (data.value.defaultPolicy) {
      return [{ ...data.value.defaultPolicy, isDefault: true }, ...policies]
    }

    return policies
  })

  return {
    monitoringPolicies,
    listMonitoringPolicies
  }
})
