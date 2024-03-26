import { PolicyRule } from '@/types/graphql'
import { Policy } from '@/types/policies'
import { createTestingPinia } from '@pinia/testing'
import { mockMonitoringPolicy, monitoringPolicyFixture } from 'tests/fixture/monitoringPolicy'
import { setActiveClient, useClient } from 'villus'

describe('Node Status Store', () => {
  beforeEach(() => {
    createTestingPinia({ stubActions: false })
    setActiveClient(useClient({ url: 'http://test/graphql' })) // Create and set a client
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('monitoringPolicies.getMonitoringPolicies complete successfully', async () => {
    const mockData = {
      value: {
        listMonitoryPolicies: monitoringPolicyFixture()
      }
    }
    vi.doMock('villus', () => ({
      useQuery: vi.fn().mockImplementation(() => ({
        data: mockData,
        execute: vi.fn().mockResolvedValue(mockData),
        isFetching: {
          value: false
        }
      }))
    }))

    const { useMonitoringPoliciesStore } = await import('@/store/Views/monitoringPoliciesStore')
    const monitoringPolicies = useMonitoringPoliciesStore()

    vi.spyOn(monitoringPolicies, 'getMonitoringPolicies')
    await monitoringPolicies.getMonitoringPolicies()

    expect(monitoringPolicies.monitoringPolicies).toStrictEqual(monitoringPolicyFixture())
  })

  it('monitoringPolicies.displayPolicyForm complete successfully', async () => {
    const { useMonitoringPoliciesStore } = await import('@/store/Views/monitoringPoliciesStore')
    const monitoringPolicies = useMonitoringPoliciesStore()
    const mockData: Policy = {isDefault: true, ...mockMonitoringPolicy}
    vi.spyOn(monitoringPolicies, 'displayPolicyForm')
    monitoringPolicies.displayPolicyForm(mockData)
    expect(monitoringPolicies.selectedPolicy).toStrictEqual(mockData)
  })

  it('monitoringPolicies.displayRuleForm complete successfully', async () => {
    const { useMonitoringPoliciesStore } = await import('@/store/Views/monitoringPoliciesStore')
    const monitoringPolicies = useMonitoringPoliciesStore()
    const mockData: PolicyRule = mockMonitoringPolicy && mockMonitoringPolicy.rules ? mockMonitoringPolicy?.rules[0] : {}
    vi.spyOn(monitoringPolicies, 'displayRuleForm')
    monitoringPolicies.displayRuleForm(mockData)
    expect(monitoringPolicies.selectedRule).toStrictEqual(mockData)
  })

  it('monitoringPolicies.resetDefaultConditions complete successfully', async () => {
    const mockData = {
      value: {
        'listAlertEventDefinitions': [
          {
            'id': 7,
            'name': 'Device Unreachable',
            'eventType': 'INTERNAL'
          },
          {
            'id': 8,
            'name': 'Device Service Restored',
            'eventType': 'INTERNAL'
          }
        ]
      }
    }
    vi.doMock('villus', () => ({
      useQuery: vi.fn().mockImplementation(() => ({
        data: mockData,
        execute: vi.fn().mockResolvedValue(mockData),
        isFetching: {
          value: false
        }
      }))
    }))
    // const { useMonitoringPoliciesQueries } = await import('@/store/Queries/monitoringPoliciesQueries')
    // const queries = useMonitoringPoliciesQueries()
    const { useMonitoringPoliciesStore } = await import('@/store/Views/monitoringPoliciesStore')
    const store = useMonitoringPoliciesStore()
    store.selectedRule = mockMonitoringPolicy && mockMonitoringPolicy.rules ? mockMonitoringPolicy?.rules[0] : {}
    vi.spyOn(store, 'resetDefaultConditions')
    await store.resetDefaultConditions()
    expect(store.resetDefaultConditions()).toBe(true)
  })
})
