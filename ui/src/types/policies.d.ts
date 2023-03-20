import { TagCreateInput } from './graphql'

export interface IPolicy {
  id: string
  name: string
  memo: string
  notifications: {
    email: boolean
    pagerDuty: boolean
    webhooks: boolean
  }
  tags: TagCreateInput[]
  rules: IRule[]
}

export interface IRule {
  id: string
  name: string
  componentType: string
  detectionMethod: string
  metricName: string
  eventTrigger?: string
  conditions: Condition[]
}

interface IObjectKeys {
  [key: string]: string | number
}

export interface ThresholdCondition extends IObjectKeys {
  id: string
  level: string
  percentage: number
  forAny: number
  durationUnit: string
  duringLast: number
  periodUnit: string
  severity: string
}

interface EventConditionBase extends IObjectKeys {
  id: string
  count: number
  time?: number
  unit?: string
  severity: string
}

export interface EventSNMPAuthFailureCondition extends EventConditionBase {}

export interface EventColdRebootCondition extends EventConditionBase {}

export interface EventPortDownCondition extends EventConditionBase {
  clearEvent?: string
}

export type EventCondition = EventSNMPAuthFailureCondition | EventColdRebootCondition | EventPortDownCondition
export type Condition = ThresholdCondition | EventCondition
