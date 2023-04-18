import MonitoringPolicies from '@/containers/MonitoringPolicies.vue'
import mount from 'tests/mountWithPiniaVillus'
import { useMonitoringPoliciesStore } from '@/store/Views/monitoringPoliciesStore'
import { useMonitoringPoliciesMutations } from '@/store/Mutations/monitoringPoliciesMutations'
import { Unknowns, SNMPEventType, ComponentType } from '@/components/MonitoringPolicies/monitoringPolicies.constants'
import { Severity } from '@/types/graphql'
import featherInputFocusDirective from '@/directives/v-focus'

const testingPayload = {
  name: 'Policy1',
  memo: '',
  notifyByEmail: false,
  notifyByPagerDuty: false,
  notifyByWebhooks: false,
  tags: [],
  rules: [
    {
      name: 'Rule1',
      componentType: ComponentType.NODE,
      triggerEvents: [
        {
          count: 1,
          severity: Severity.Critical,
          triggerEvent: SNMPEventType.SNMP_COLD_START,
          overtimeUnit: Unknowns.UNKNOWN_UNIT,
          clearEvent: Unknowns.UNKNOWN_EVENT
        }
      ]
    }
  ]
}

const wrapper = mount({
  component: MonitoringPolicies,
  shallow: false,
  stubActions: false,
  global: {
    directives: {
      focus: featherInputFocusDirective
    }
  }
})

test('The Monitoring Policies page container mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})

test('The store populates with a selected policy when "New Policy" is clicked.', async () => {
  const store = useMonitoringPoliciesStore()
  const newPolicyBtn = wrapper.get('[data-test="new-policy-btn"]')

  expect(store.selectedPolicy).toBeUndefined()
  await newPolicyBtn.trigger('click')
  expect(store.displayPolicyForm).toHaveBeenCalledTimes(1)
  expect(store.selectedPolicy).toBeTruthy()
})

test('The store populates with a selected rule when "New Rule" is clicked.', async () => {
  const store = useMonitoringPoliciesStore()
  const newRuleBtn = wrapper.get('[data-test="new-rule-btn"]')

  expect(store.selectedRule).toBeUndefined()
  await newRuleBtn.trigger('click')
  expect(store.displayRuleForm).toHaveBeenCalledTimes(1)
  expect(store.selectedRule).toBeTruthy()
})

test('Saving a rule to the policy.', async () => {
  const store = useMonitoringPoliciesStore()
  const saveRuleBtn = wrapper.get('[data-test="save-rule-btn"]')

  expect(store.selectedPolicy!.rules.length).toBe(0)
  await wrapper.get('[data-test="rule-name-input"] .feather-input').setValue('Rule1')
  await saveRuleBtn.trigger('click')

  expect(store.saveRule).toHaveBeenCalledTimes(1)
  expect(store.selectedPolicy!.rules.length).toBe(1)
})

test('Saving a new policy.', async () => {
  const store = useMonitoringPoliciesStore()
  const mutations = useMonitoringPoliciesMutations()
  const savePolicyBtn = wrapper.get('[data-test="save-policy-btn"]')

  await wrapper.get('[data-test="policy-name-input"] .feather-input').setValue('Policy1')
  await savePolicyBtn.trigger('click')

  expect(store.savePolicy).toHaveBeenCalledTimes(1)
  expect(mutations.addMonitoringPolicy).toHaveBeenCalledTimes(1)
  expect(mutations.addMonitoringPolicy).toHaveBeenCalledWith({ policy: testingPayload })
})

test('Clicking edit populates the selected policy for editing', async () => {
  const existingPolicy = { ...testingPayload, id: 1 }
  const store = useMonitoringPoliciesStore()
  store.selectedPolicy = undefined
  store.selectedRule = undefined
  store.monitoringPolicies = [existingPolicy]

  await nextTick()
  const editPolicyBtn = wrapper.get('[data-test="policy-edit-btn"]')
  await editPolicyBtn.trigger('click')

  expect(store.selectedPolicy!.id).toBe(1)
  expect(store.selectedPolicy!.name).toBe('Policy1')
})

test('Clicking copy populates the selected policy with a copy', async () => {
  const existingPolicy = { ...testingPayload, id: 1 }
  const store = useMonitoringPoliciesStore()
  store.selectedPolicy = undefined
  store.selectedRule = undefined
  store.monitoringPolicies = [existingPolicy]

  await nextTick()
  const copyPolicyBtn = wrapper.get('[data-test="policy-copy-btn"]')
  await copyPolicyBtn.trigger('click')

  expect(store.selectedPolicy!.id).toBeUndefined()
  expect(store.selectedPolicy!.name).toBeUndefined()
  expect(store.selectedPolicy!.rules[0].name).toBe('Rule1')
})
