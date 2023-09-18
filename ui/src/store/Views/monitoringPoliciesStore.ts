import { defineStore } from 'pinia'
import { cloneDeep, findIndex } from 'lodash'
import { Condition, Policy, ThresholdCondition } from '@/types/policies'
import { useMonitoringPoliciesMutations } from '../Mutations/monitoringPoliciesMutations'
import { useMonitoringPoliciesQueries } from '../Queries/monitoringPoliciesQueries'
import useSnackbar from '@/composables/useSnackbar'
import { ThresholdLevels, Unknowns } from '@/components/MonitoringPolicies/monitoringPolicies.constants'
import {
  AlertCondition,
  DetectionMethod,
  EventType,
  ManagedObjectType,
  MonitorPolicy,
  PolicyRule,
  Severity,
  TimeRangeUnit
} from '@/types/graphql'
import { useAlertEventDefinitionQueries } from '@/store/Queries/alertEventDefinitionQueries'

const { showSnackbar } = useSnackbar()

type TState = {
  selectedPolicy?: Policy
  selectedRule?: PolicyRule
  monitoringPolicies: MonitorPolicy[]
}

const defaultPolicy: Policy = {
  name: '',
  memo: '',
  notifyByEmail: false,
  notifyByPagerDuty: false,
  notifyByWebhooks: false,
  tags: ['default'],
  rules: []
}

function getDefaultThresholdCondition(): ThresholdCondition {
  return {
    id: new Date().getTime(),
    level: ThresholdLevels.ABOVE,
    percentage: 50,
    forAny: 5,
    durationUnit: TimeRangeUnit.Second,
    duringLast: 60,
    periodUnit: TimeRangeUnit.Second,
    severity: Severity.Critical
  }
}

async function getDefaultEventCondition(): Promise<AlertCondition> {
  const alertEventDefinitionQueries = useAlertEventDefinitionQueries()
  const alertEventDefinitions = await alertEventDefinitionQueries.listAlertEventDefinitions(EventType.SnmpTrap)
  if (alertEventDefinitions.value?.listAlertEventDefinitions?.length) {
    return {
      id: new Date().getTime(),
      count: 1,
      severity: Severity.Critical,
      overtimeUnit: Unknowns.UNKNOWN_UNIT,
      triggerEvent: alertEventDefinitions.value.listAlertEventDefinitions[0]
    }
  } else {
    throw Error("Can't load alertEventDefinitions")
  }
}

async function getDefaultRule(): Promise<PolicyRule> {
  return {
    id: new Date().getTime(),
    name: '',
    componentType: ManagedObjectType.Node,
    detectionMethod: DetectionMethod.Event,
    eventType: EventType.SnmpTrap,
    alertConditions: [await getDefaultEventCondition()]
  }
}

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
    async displayRuleForm(rule?: PolicyRule) {
      this.selectedRule = rule ? cloneDeep(rule) : await getDefaultRule()
    },
    async resetDefaultConditions() {
      if (!this.selectedRule) return

      // detection method THRESHOLD
      if (this.selectedRule.detectionMethod === DetectionMethod.Threshold) {
        return (this.selectedRule.alertConditions = [getDefaultThresholdCondition()])
      }

      // detection method EVENT
      return (this.selectedRule.alertConditions = [await getDefaultEventCondition()])
    },
    async addNewCondition() {
      if (!this.selectedRule) return

      // detection method THRESHOLD
      if (this.selectedRule.detectionMethod === DetectionMethod.Threshold) {
        return this.selectedRule.alertConditions?.push(getDefaultThresholdCondition())
      }

      // detection method EVENT
      return this.selectedRule.alertConditions?.push(await getDefaultEventCondition())
    },
    updateCondition(id: string, condition: Condition) {
      this.selectedRule!.alertConditions?.map((currentCondition: AlertCondition) => {
        if (currentCondition.id === id) {
          return { ...currentCondition, ...condition }
        }
        return
      })
    },
    deleteCondition(id: string) {
      this.selectedRule!.alertConditions = this.selectedRule!.alertConditions?.filter(
        (c: AlertCondition) => c.id !== id
      )
    },
    async saveRule() {
      const existingItemIndex = findIndex(this.selectedPolicy!.rules, { id: this.selectedRule!.id })

      if (existingItemIndex !== -1) {
        // replace existing rule
        this.selectedPolicy!.rules?.splice(existingItemIndex, 1, this.selectedRule!)
      } else {
        // add new rule
        this.selectedPolicy!.rules?.push(this.selectedRule!)
      }

      this.selectedRule = await getDefaultRule()
      showSnackbar({ msg: 'Rule successfully applied to the policy.' })
    },
    async savePolicy() {
      const { addMonitoringPolicy, error } = useMonitoringPoliciesMutations()

      // modify payload to comply with current BE format
      const policy = cloneDeep(this.selectedPolicy!)
      policy.rules = policy.rules?.map((rule) => {
        rule.alertConditions = rule.alertConditions?.map((condition) => {
          if (!policy.id) delete condition.id // don't send generated ids
          return condition
        })
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
    },
    removeRule() {
      const ruleIndex = findIndex(this.selectedPolicy!.rules, { id: this.selectedRule!.id })

      if (ruleIndex !== -1) {
        this.selectedPolicy!.rules?.splice(ruleIndex, 1)
      }

      this.selectedRule = undefined
    }
  }
})
