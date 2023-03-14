import { defineStore } from 'pinia'
import { cloneDeep, findIndex } from 'lodash'
import { IPolicy, IRule } from '@/types/policies'
import { useMonitoringPoliciesMutations } from '../Mutations/monitoringPolicies'
import { useMonitoringPoliciesQueries } from '../Queries/monitoringPoliciesQueries'

type TState = {
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

const getDefaultRule = () => ({
  id: new Date().getTime().toString(),
  name: '',
  componentType: 'cpu',
  detectionMethod: 'thresholdAlert',
  metricName: 'interfaceUtil',
  conditions: []
})

export const useMonitoringPoliciesStore = defineStore('monitoringPoliciesStore', {
  state: (): TState => ({
    selectedPolicy: undefined,
    selectedRule: undefined
  }),
  actions: {
    displayPolicyForm(policy?: IPolicy) {
      this.selectedPolicy = policy || cloneDeep(defaultPolicy)
      if (!policy) this.selectedRule = undefined
    },
    displayRuleForm(rule?: IRule) {
      this.selectedRule = cloneDeep(rule) || getDefaultRule()
    },
    addNewCondition() {
      const defaultCondition = {
        id: new Date().getTime().toString(),
        level: 'above',
        percentage: 50,
        duration: '5s',
        period: '15s',
        severity: 'critical'
      }
      this.selectedRule!.conditions.push(defaultCondition)
    },
    removeCondition(id: string) {
      this.selectedRule!.conditions = this.selectedRule!.conditions.filter((c) => c.id !== id)
    },
    saveRule() {
      const existingItemIndex = findIndex(this.selectedPolicy!.rules, { id: this.selectedRule?.id })

      if (existingItemIndex !== -1) { 
        // replace existing rule
        this.selectedPolicy!.rules.splice(existingItemIndex, 1, this.selectedRule!)
      } else { 
        // add new rule
        this.selectedPolicy!.rules.push(this.selectedRule!)
      }

      this.selectedRule = getDefaultRule()
    },
    async savePolicy() {
      const { addMonitoringPolicy, error } = useMonitoringPoliciesMutations()
      const { listMonitoringPolicies } = useMonitoringPoliciesQueries()

      await addMonitoringPolicy({ policy: this.selectedPolicy! })

      if (!error.value) {
        this.selectedPolicy = cloneDeep(defaultPolicy)
        listMonitoringPolicies()
      }

      return !error.value
    }
  }
})
