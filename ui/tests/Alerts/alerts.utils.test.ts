import { getCountMap } from '@/components/Alerts/alerts.utils'
import { Ack, Severities, Statuses } from '@/components/Alerts/alerts.constants'
import { AlertCountByType, AlertCountType, Severity } from '@/types/graphql'

const types: AlertCountByType[] = [
  {
    countType: AlertCountType.CountAcknowledged,
    count: 5
  },
  {
    countType: AlertCountType.CountCritical,
    count: 10
  },
  {
    countType: AlertCountType.CountMinor,
    count: 3
  }
]


test('Should populate the counts map correctly', () => {
  const map = getCountMap(types, Severities, Statuses)
  expect(map[Severity.Critical]).toBe(10)
  expect(map[Severity.Minor]).toBe(3)
  expect(map[Ack]).toBe(5)
})
