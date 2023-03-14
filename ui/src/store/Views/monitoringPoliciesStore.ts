import { defineStore } from 'pinia'
import { cloneDeep } from 'lodash'
import { IPolicy, IRule } from '@/types/policies'

type TState = {
  existingPolicies: IPolicy[]
  existingRules: IRule[]
  selectedPolicy?: IPolicy
  selectedRule?: IRule
}

const defaultPolicy: IPolicy = {
  id: '',
  name: '',
  memo: '',
  notifications: {
    email: false,
    pagerDuty: false,
    webhooks: false
  },
  tags: [],
  rules: []
}

const defaultRule: IRule = {
  id: '',
  name: '',
  componentType: 'cpu',
  detectionMethod: 'thresholdAlert',
  metricName: 'interfaceUtil',
  conditions: []
}

export const useMonitoringPoliciesStore = defineStore('monitoringPoliciesStore', {
  state: (): TState => ({
    existingRules: [],
    existingPolicies: [],
    selectedPolicy: undefined,
    selectedRule: undefined
  }),
  actions: {
    displayPolicyForm(policy?: IPolicy) {
      this.selectedPolicy = policy || cloneDeep(defaultPolicy)
      if (!policy) this.selectedRule = undefined
    },
    displayRuleForm(rule?: IRule) {
      this.selectedRule = rule || cloneDeep(defaultRule)
    },
    setMetricName(name: string) {
      this.selectedRule!.metricName = name
    },
    addNewCondition() {
      const defaultCondition = {
        id: new Date().getTime(),
        level: 'above',
        percentage: 50,
        duration: 5,
        period: 15,
        severity: 'critical'
      }
      this.selectedRule!.conditions.push(defaultCondition)
    },
    removeCondition(id: number) {
      this.selectedRule!.conditions = this.selectedRule!.conditions.filter((c) => c.id !== id)
    },
    saveRule() {
      this.selectedPolicy!.rules.push(this.selectedRule!)
      this.existingRules.push(this.selectedRule!)
      this.selectedRule = cloneDeep(defaultRule) // clear form
    },
    savePolicy() {
      this.existingPolicies.push(this.selectedPolicy!)
      this.selectedPolicy = cloneDeep(defaultPolicy) // clear form
    }
  }
})
