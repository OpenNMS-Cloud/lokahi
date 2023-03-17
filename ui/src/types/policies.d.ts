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
  conditions: ICondition[]
}

interface IObjectKeys {
  [key: string]: string | number
}

export interface ICondition extends IObjectKeys {
  id: string
  level: string
  percentage: number
  forAny: number
  durationUnit: string
  duringLast: number
  periodUnit: string
  severity: string
}
