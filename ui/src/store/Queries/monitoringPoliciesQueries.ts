import { IPolicy } from '@/types/policies'
import { defineStore } from 'pinia'
import { useQuery } from 'villus'
// import { ListMonitoringPoliciesDocument } from '@/types/graphql'

export const useMonitoringPoliciesQueries = defineStore('monitoringPoliciesQueries', () => {

  const mockData: IPolicy[] = [
    {
      id: '1',
      name: 'Servers',
      memo: 'Servers memo',
      notifications: {
        email: true,
        pagerDuty: false,
        webhooks: false
      },
      tags: [{
        name: 'server'
      }],
      rules: [
        {
          id: '1',
          name: 'Saturation',
          componentType: 'CPU',
          detectionMethod: 'threshold',
          metricName: 'saturation',
          conditions: [{
            id: '1',
            level: 'above',
            percentage: 90,
            duration: '3 minutes',
            period: '10 minutes',
            severity: 'critical'
          }]
        },
        {
          id: '2',
          name: 'errors',
          componentType: 'interface',
          detectionMethod: 'events',
          metricName: 'errors',
          conditions: [{
            id: '1',
            level: 'above',
            percentage: 90,
            duration: '3 minutes',
            period: '10 minutes',
            severity: 'critical'
          }]
        },
      ]
    }
  ]

  // const { data: monitoringPolicies } = useQuery({ query: ListMonitoringPoliciesDocument })

  return {
    // monitoringPolicies: computed(() => monitoringPolicies.value?.listMonitoringPolicies || [])
    monitoringPolicies: mockData
  }
})


