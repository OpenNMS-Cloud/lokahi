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
  ThresholdLevels
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
  severity: Severity.Critical
})

// port down event has an extra properties
const getDefaultEventConditionPortDown = () => ({
  ...getDefaultEventCondition(),
  clearEvent: SNMPEventType.PORT_UP
})

const getDefaultRule = () => ({
  id: new Date().getTime(),
  name: '',
  componentType: ComponentType.CPU,
  detectionMethod: DetectionMethodTypes.EVENT,
  metricName: EventMetrics.SNMP_TRAP,
  triggerEvent: SNMPEventType.COLD_REBOOT,
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
      this.selectedPolicy = policy || cloneDeep(defaultPolicy)
      if (!policy) this.selectedRule = undefined
    },
    displayRuleForm(rule?: Rule) {
      if (rule) {
        const { detectionMethod, metricName } = getDefaultRule()
        this.selectedRule = { ...cloneDeep(rule), detectionMethod, metricName }
      } else {
        this.selectedRule = getDefaultRule()
      }
      // BE not ready so must add default above
      // this.selectedRule = cloneDeep(rule) || getDefaultRule()
    },
    resetDefaultConditions() {
      if (!this.selectedRule) return

      // detection method THRESHOLD
      if (this.selectedRule.detectionMethod === DetectionMethodTypes.THRESHOLD) {
        return (this.selectedRule.triggerEvents = [getDefaultThresholdCondition()])
      }

      // detection method EVENT
      if (this.selectedRule.detectionMethod === DetectionMethodTypes.EVENT) {
        this.selectedRule.triggerEvent === SNMPEventType.PORT_DOWN
          ? (this.selectedRule.triggerEvents = [getDefaultEventConditionPortDown()])
          : (this.selectedRule.triggerEvents = [getDefaultEventCondition()])
      }
    },
    addNewCondition() {
      if (!this.selectedRule) return

      // detection method THRESHOLD
      if (this.selectedRule.detectionMethod === DetectionMethodTypes.THRESHOLD) {
        return this.selectedRule.triggerEvents.push(getDefaultThresholdCondition())
      }

      // detection method EVENT
      if (this.selectedRule.detectionMethod === DetectionMethodTypes.EVENT) {
        this.selectedRule.triggerEvent === SNMPEventType.PORT_DOWN
          ? this.selectedRule.triggerEvents.push(getDefaultEventConditionPortDown())
          : this.selectedRule.triggerEvents.push(getDefaultEventCondition())
      }
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
          condition.triggerEvent = rule.triggerEvent
          if (!policy.id) delete condition.id // don't send generated ids
          return condition
        })
        delete rule.triggerEvent
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
    }
  }
})
