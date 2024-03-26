import { DetectionMethod, EventType, ManagedObjectType, MonitorPolicy } from '@/types/graphql'

export const mockMonitoringPolicy: MonitorPolicy = {
  __typename: 'MonitorPolicy',
  id: 1,
  memo: 'Dummy memo',
  name: 'Dummy Policy',
  notifyByEmail: true,
  notifyByPagerDuty: false,
  notifyByWebhooks: true,
  notifyInstruction: 'Notify immediately',
  rules: [
    {
      __typename: 'PolicyRule',
      alertConditions: [
        {
          __typename: 'AlertCondition',
          clearEvent: {
            __typename: 'AlertEventDefinition',
            clearKey: 'dummy_clear_key',
            eventType: EventType.Internal,
            id: 1,
            name: 'Dummy Alert Event',
            reductionKey: 'dummy_reduction_key',
            uei: 'dummy_uei'
          },
          count: 5,
          id: 1,
          overtime: 10,
          overtimeUnit: 'minutes',
          severity: 'high',
          triggerEvent: {
            __typename: 'AlertEventDefinition',
            clearKey: 'dummy_clear_key',
            eventType: EventType.Internal,
            id: 1,
            name: 'Dummy Alert Event',
            reductionKey: 'dummy_reduction_key',
            uei: 'dummy_uei'
          }
        }
      ],
      componentType: ManagedObjectType.Node,
      detectionMethod: DetectionMethod.Event,
      eventType: EventType.Internal,
      id: 1,
      name: 'Dummy Rule',
      thresholdMetricName: 'Dummy Metric'
    }
  ],
  tags: ['tag1', 'tag2']
}

export const monitoringPolicyFixture = (props: Partial<MonitorPolicy> = {}) => ([{ ...mockMonitoringPolicy, ...props }])
