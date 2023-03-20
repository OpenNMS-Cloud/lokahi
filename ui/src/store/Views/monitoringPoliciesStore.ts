import { defineStore } from 'pinia'
import { cloneDeep, findIndex } from 'lodash'
import { IPolicy, IRule, Condition } from '@/types/policies'
import { useMonitoringPoliciesMutations } from '../Mutations/monitoringPoliciesMutations'
import { useMonitoringPoliciesQueries } from '../Queries/monitoringPoliciesQueries'
import useSnackbar from '@/composables/useSnackbar'

const { showSnackbar } = useSnackbar()

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

const getDefaultThresholdCondition = () => ({
  id: new Date().getTime().toString(),
  level: 'above',
  percentage: 50,
  forAny: 5,
  durationUnit: 'seconds',
  duringLast: 60,
  periodUnit: 'seconds',
  severity: 'critical'
})

const getDefaultRule = () => ({
  id: new Date().getTime().toString(),
  name: '',
  componentType: 'cpu',
  detectionMethod: 'threshold',
  metricName: 'over-utilization',
  eventTrigger: undefined,
  conditions: [getDefaultThresholdCondition()]
})

export const useMonitoringPoliciesStore = defineStore('monitoringPoliciesStore', {
  state: (): TState => ({
    selectedPolicy: undefined,
    selectedRule: undefined
  }),
  getters: {
    monitoringPolicies() {
      const { monitoringPolicies } = useMonitoringPoliciesQueries()
      return monitoringPolicies
    }
  },
  actions: {
    displayPolicyForm(policy?: IPolicy) {
      this.selectedPolicy = policy || cloneDeep(defaultPolicy)
      if (!policy) this.selectedRule = undefined
    },
    displayRuleForm(rule?: IRule) {
      this.selectedRule = cloneDeep(rule) || getDefaultRule()
    },
    addNewThresholdCondition() {
      this.selectedRule!.conditions.push(getDefaultThresholdCondition())
    },
    updateCondition(id: string, condition: Condition) {
      this.selectedRule!.conditions.map((currentCondition) => {
        if (currentCondition.id === id) {
          return {...currentCondition, ...condition}
        }
        return
      })
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
      showSnackbar({ msg: 'Rule successfully applied to the policy.' })
    },
    async savePolicy() {
      const { addMonitoringPolicy, error } = useMonitoringPoliciesMutations()
      const { listMonitoringPolicies } = useMonitoringPoliciesQueries()

      await addMonitoringPolicy({ policy: this.selectedPolicy! })

      if (!error.value) {
        this.selectedPolicy = undefined
        this.selectedRule = undefined
        listMonitoringPolicies()
        showSnackbar({ msg: 'Policy successfully applied.' })
      }

      return !error.value
    }
  }
})
