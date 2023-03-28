import { MonitorPolicy, PolicyRule, TriggerEvent } from './graphql'

export interface Policy extends MonitorPolicy {
  rules: Rule[]
}

export interface Rule extends PolicyRule {
  detectionMethod?: string
  metricName?: string
  triggerEvent?: string
  triggerEvents: Condition[]
}

interface IObjectKeys {
  [key: string]: string | number
}

export interface ThresholdCondition extends IObjectKeys {
  id: number
  level: string
  percentage: number
  forAny: number
  durationUnit: string
  duringLast: number
  periodUnit: string
  severity: string
}

export type EventCondition = TriggerEvent & IObjectKeys
export type Condition = ThresholdCondition | EventCondition
