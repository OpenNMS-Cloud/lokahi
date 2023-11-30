import { AlertCount, Severity } from '@/types/graphql'
import { Ack, UnAck, DefaultCountMap } from './alerts.constants'

export const getCountMap = (
  alertCount: AlertCount = { acknowledgedCount: 0, totalAlertCount: 0, countBySeverity: {} },
  severities: Severity[] = []
) => {
  const map: any = {}
  const countsBySeverity = alertCount.countBySeverity

  for (const key in countsBySeverity) {
    for (const item of severities) {
      if (key.toUpperCase().includes(item.toUpperCase())) {
        map[item] = countsBySeverity[key]
      }
    }
  }

  map[Ack] = alertCount.acknowledgedCount
  map[UnAck] = alertCount.totalAlertCount - alertCount.acknowledgedCount

  return { ...DefaultCountMap, ...map }
}
