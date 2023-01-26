import { defineStore } from 'pinia'
import { without } from 'lodash'
import { IPolicy, IRule } from '@/types/policies'

type TState = {
  selectedPolicy: IPolicy
  selectedRule: IRule
}

export const useMonitoringPoliciesStore = defineStore('monitoringPoliciesStore', {
  state: (): TState => ({
    selectedPolicy: {
      id: '',
      name: '',
      tags: [],
      rules: []
    },
    selectedRule: {
      id: '',
      name: '',
      componentType: 'cpu',
      detectionMethod: 'thresholdAlert',
      metricName: 'interfaceUtil',
      conditions: []
    }
  }),
  actions: {
    removeTag(tag: string) {
      this.selectedPolicy.tags = without(this.selectedPolicy.tags, tag)
    },
    setMetricName(name: string) {
      this.selectedRule.metricName = name
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
      this.selectedRule.conditions.push(defaultCondition)
    },
    removeCondition(id: number) {
      this.selectedRule.conditions = this.selectedRule.conditions.filter((c) => c.id !== id)
    }
  }
})
