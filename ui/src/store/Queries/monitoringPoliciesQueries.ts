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
            percentage: 50,
            forAny: 10,
            durationUnit: 'seconds',
            duringLast: 15,
            periodUnit: 'minutes',
            severity: 'major'
          },{
            id: '2',
            level: 'above',
            percentage: 50,
            forAny: 10,
            durationUnit: 'minutes',
            duringLast: 15,
            periodUnit: 'minutes',
            severity: 'critical'
          }]
        },
        {
          id: '2',
          name: 'errors',
          componentType: 'interface',
          detectionMethod: 'event',
          metricName: 'errors',
          conditions: [{
            id: '1',
            level: 'above',
            percentage: 90,
            forAny: 10,
            durationUnit: 'seconds',
            duringLast: 15,
            periodUnit: 'minutes',
            severity: 'critical'
          }]
        },
      ]
    }
  ]

  // const { data: monitoringPolicies, execute: listMonitoringPolicies } = useQuery({ query: ListMonitoringPoliciesDocument })

  return {
    // monitoringPolicies: computed(() => monitoringPolicies.value?.listMonitoringPolicies || [])
    // listMonitoringPolicies
    monitoringPolicies: mockData,
    listMonitoringPolicies: () => mockData
  }
})


