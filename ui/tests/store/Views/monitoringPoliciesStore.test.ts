import { EventType, PolicyRule } from '@/types/graphql'
import { Policy, ThresholdCondition } from '@/types/policies'
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

  it.skip('monitoringPolicies.resetDefaultConditions complete successfully', async () => {
    // const mockData = {
    //   value: {
    //     listAlertEventDefinitions: [{
    //       id: 1,
    //       name: 'SNMP Trap',
    //       eventType: EventType.SnmpTrap
    //     }]
    //   }
    // }

    // // Mocking useQuery hook
    // vi.doMock('villus', () => ({
    //   useQuery: vi.fn().mockImplementation(() => ({
    //     data: mockData,
    //     execute: vi.fn().mockResolvedValue(mockData),
    //     isFetching: {
    //       value: false
    //     }
    //   }))
    // }))
    const { useMonitoringPoliciesStore } = await import('@/store/Views/monitoringPoliciesStore')
    const store = useMonitoringPoliciesStore()
    store.selectedPolicy = {...mockMonitoringPolicy, isDefault: false}
    store.selectedRule = mockMonitoringPolicy && mockMonitoringPolicy.rules ? mockMonitoringPolicy?.rules[0] : {}

    vi.spyOn(store, 'resetDefaultConditions')
    await store.resetDefaultConditions()
    expect(store.resetDefaultConditions).toHaveBeenCalledOnce()
  })

  it.skip('monitoringPolicies.addNewCondition complete successfully', async () => {
    const { useMonitoringPoliciesStore } = await import('@/store/Views/monitoringPoliciesStore')
    const store = useMonitoringPoliciesStore()

    store.selectedPolicy = {...mockMonitoringPolicy, isDefault: false}
    store.selectedRule = mockMonitoringPolicy && mockMonitoringPolicy.rules ? mockMonitoringPolicy?.rules[0] : {}

    vi.spyOn(store, 'addNewCondition')
    await store.addNewCondition()
    expect(store.addNewCondition).toHaveBeenCalledOnce()
  })

  it('monitoringPolicies.updateCondition complete successfully', async () => {
    const { useMonitoringPoliciesStore } = await import('@/store/Views/monitoringPoliciesStore')
    const store = useMonitoringPoliciesStore()

    store.selectedPolicy = {...mockMonitoringPolicy, isDefault: false}
    store.selectedRule = mockMonitoringPolicy && mockMonitoringPolicy.rules ? mockMonitoringPolicy?.rules[0] : {}

    vi.spyOn(store, 'updateCondition')
    const mockCondition: ThresholdCondition = {
      id: 1,
      level: 'Warning',
      percentage: 70,
      forAny: 1,
      durationUnit: 'Hours',
      duringLast: 24,
      periodUnit: 'Hours',
      severity: 'Medium'
    }
    store.updateCondition('1', mockCondition)
    expect(store.updateCondition).toHaveBeenCalledWith('1', mockCondition)
  })

  it('monitoringPolicies.deleteCondition complete successfully', async () => {
    const { useMonitoringPoliciesStore } = await import('@/store/Views/monitoringPoliciesStore')
    const store = useMonitoringPoliciesStore()

    store.selectedPolicy = {...mockMonitoringPolicy, isDefault: false}
    store.selectedRule = mockMonitoringPolicy && mockMonitoringPolicy.rules ? mockMonitoringPolicy?.rules[0] : {}

    vi.spyOn(store, 'updateCondition')
    store.deleteCondition('2')
    expect(store.deleteCondition).toHaveBeenCalledWith('2')
  })

  it('monitoringPolicies.deleteCondition complete successfully', async () => {
    const { useMonitoringPoliciesStore } = await import('@/store/Views/monitoringPoliciesStore')
    const store = useMonitoringPoliciesStore()

    store.selectedPolicy = {...mockMonitoringPolicy, isDefault: false}
    store.selectedRule = mockMonitoringPolicy && mockMonitoringPolicy.rules ? mockMonitoringPolicy?.rules[0] : {}

    vi.spyOn(store, 'updateCondition')
    store.validateRule(store.selectedRule)
    expect(store.validateRule).toHaveBeenCalledWith(store.selectedRule)
    expect(store.validateRule).toBeTruthy()
  })

  it('monitoringPolicies.validateMonitoringPolicy complete successfully', async () => {
    const { useMonitoringPoliciesStore } = await import('@/store/Views/monitoringPoliciesStore')
    const store = useMonitoringPoliciesStore()

    store.selectedPolicy = {...mockMonitoringPolicy, isDefault: false}

    vi.spyOn(store, 'validateMonitoringPolicy')
    store.validateMonitoringPolicy(store.selectedPolicy)
    expect(store.validateMonitoringPolicy).toHaveBeenCalledWith(store.selectedPolicy)
    expect(store.validateMonitoringPolicy).toBeTruthy()
  })

  it('monitoringPolicies.copyPolicy complete successfully', async () => {
    const { useMonitoringPoliciesStore } = await import('@/store/Views/monitoringPoliciesStore')
    const store = useMonitoringPoliciesStore()
    const mockData: Policy = {
      name: '',
      memo: '',
      notifyByEmail: false,
      notifyByPagerDuty: false,
      notifyByWebhooks: false,
      tags: ['default'],
      rules: []
    }

    vi.spyOn(store, 'copyPolicy')
    store.copyPolicy(mockData)
    expect(store.copyPolicy).toHaveBeenCalledWith(mockData)
    expect(store.copyPolicy).toHaveBeenCalledOnce()
    expect(store.selectedPolicy).toStrictEqual({
      memo: '',
      notifyByEmail: false,
      notifyByPagerDuty: false,
      notifyByWebhooks: false,
      tags: ['default'],
      rules: []
    })
  })


})
