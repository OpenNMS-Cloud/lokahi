export interface IPolicy {
  id: string
  name: string
  tags: string[]
  rules: IRule[]
}

export interface IRule {
  id: string
  name: string
  componentType: string
  detectionMethod: string
  metricName: string
  conditions: ICondition[]
}

interface IObjectKeys {
  [key: string]: string | number
}

export interface ICondition extends IObjectKeys {
  id: number
  level: string
  percentage: number
  duration: number
  period: number
  severity: string
}
