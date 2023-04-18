import { defineStore } from 'pinia'
import { cloneDeep, findIndex } from 'lodash'
import { Policy, Rule, Condition } from '@/types/policies'
import { useMonitoringPoliciesMutations } from '../Mutations/monitoringPoliciesMutations'
import { useMonitoringPoliciesQueries } from '../Queries/monitoringPoliciesQueries'
import useSnackbar from '@/composables/useSnackbar'
import {
  DetectionMethodTypes,
  SNMPEventType,
  ComponentType,
  EventMetrics,
  ThresholdLevels,
  Unknowns
} from '@/components/MonitoringPolicies/monitoringPolicies.constants'
import { MonitorPolicy } from '@/types/graphql'
import { Severity, TimeRangeUnit } from '@/types/graphql'

const { showSnackbar } = useSnackbar()

type TState = {
  selectedPolicy?: Policy
  selectedRule?: Rule
  monitoringPolicies: MonitorPolicy[]
}

const defaultPolicy: Policy = {
  id: undefined,
  name: '',
  memo: '',
  notifyByEmail: false,
  notifyByPagerDuty: false,
  notifyByWebhooks: false,
  tags: [],
  rules: []
}

const getDefaultThresholdCondition = () => ({
  id: new Date().getTime(),
  level: ThresholdLevels.ABOVE,
  percentage: 50,
  forAny: 5,
  durationUnit: TimeRangeUnit.Second,
  duringLast: 60,
  periodUnit: TimeRangeUnit.Second,
  severity: Severity.Critical
})

const getDefaultEventCondition = () => ({
  id: new Date().getTime(),
  count: 1,
  severity: Severity.Critical,
  overtimeUnit: Unknowns.UNKNOWN_UNIT,
  triggerEvent: SNMPEventType.SNMP_COLD_START,
  clearEvent: Unknowns.UNKNOWN_EVENT
})

const getDefaultRule = () => ({
  id: new Date().getTime(),
  name: '',
  componentType: ComponentType.NODE,
  detectionMethod: DetectionMethodTypes.EVENT,
  metricName: EventMetrics.SNMP_TRAP,
  triggerEvents: [getDefaultEventCondition()]
})

export const useMonitoringPoliciesStore = defineStore('monitoringPoliciesStore', {
  state: (): TState => ({
    selectedPolicy: undefined,
    selectedRule: undefined,
    monitoringPolicies: []
  }),
  actions: {
    // used for initial population of policies
    async getMonitoringPolicies() {
      const queries = useMonitoringPoliciesQueries()
      await queries.listMonitoringPolicies()
      this.monitoringPolicies = queries.monitoringPolicies
    },
    displayPolicyForm(policy?: Policy) {
      this.selectedPolicy = policy ? cloneDeep(policy) : cloneDeep(defaultPolicy)
      this.selectedRule = undefined
    },
    displayRuleForm(rule?: Rule) {
      this.selectedRule = rule ? cloneDeep(rule) : getDefaultRule()
    },
    resetDefaultConditions() {
      if (!this.selectedRule) return

      // detection method THRESHOLD
      if (this.selectedRule.detectionMethod === DetectionMethodTypes.THRESHOLD) {
        return (this.selectedRule.triggerEvents = [getDefaultThresholdCondition()])
      }

      // detection method EVENT
      return (this.selectedRule.triggerEvents = [getDefaultEventCondition()])
    },
    addNewCondition() {
      if (!this.selectedRule) return

      // detection method THRESHOLD
      if (this.selectedRule.detectionMethod === DetectionMethodTypes.THRESHOLD) {
        return this.selectedRule.triggerEvents.push(getDefaultThresholdCondition())
      }

      // detection method EVENT
      return this.selectedRule.triggerEvents.push(getDefaultEventCondition())
    },
    updateCondition(id: string, condition: Condition) {
      this.selectedRule!.triggerEvents.map((currentCondition) => {
        if (currentCondition.id === id) {
          return { ...currentCondition, ...condition }
        }
        return
      })
    },
    deleteCondition(id: string) {
      this.selectedRule!.triggerEvents = this.selectedRule!.triggerEvents.filter((c) => c.id !== id)
    },
    saveRule() {
      const existingItemIndex = findIndex(this.selectedPolicy!.rules, { id: this.selectedRule!.id })

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

      // modify payload to comply with current BE format
      const policy = cloneDeep(this.selectedPolicy!)
      policy.rules = policy.rules.map((rule) => {
        rule.triggerEvents = rule.triggerEvents.map((condition) => {
          if (!policy.id) delete condition.id // don't send generated ids
          return condition
        })
        delete rule.detectionMethod
        delete rule.metricName
        if (!policy.id) delete rule.id // don't send generated ids
        return rule
      })

      await addMonitoringPolicy({ policy: policy })

      if (!error.value) {
        this.selectedPolicy = undefined
        this.selectedRule = undefined
        this.getMonitoringPolicies()
        showSnackbar({ msg: 'Policy successfully applied.' })
      }

      return !error.value
    },
    copyPolicy(policy: Policy) {
      const copiedPolicy = cloneDeep(policy)
      delete copiedPolicy.isDefault
      delete copiedPolicy.id
      delete copiedPolicy.name
      this.displayPolicyForm(copiedPolicy)
    }
  }
})
