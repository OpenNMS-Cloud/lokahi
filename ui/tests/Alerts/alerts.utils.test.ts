import { getCountMap } from '@/components/Alerts/alerts.utils'
import { Ack, Severities, UnAck } from '@/components/Alerts/alerts.constants'
import { AlertCount, Severity } from '@/types/graphql'

const alertCount: AlertCount = {
  acknowledgedCount: 12,
  totalAlertCount: 15,
  countBySeverity: {
    major: 1,
    critical: 4,
    MINOR: 5
  }
}

test('Should populate the counts map correctly', () => {
  const map = getCountMap(alertCount, Severities)
  expect(map[Ack]).toBe(12)
  expect(map[UnAck]).toBe(3)
  expect(map[Severity.Critical]).toBe(4)
  expect(map[Severity.Major]).toBe(1)
  expect(map[Severity.Minor]).toBe(5)
  expect(map[Severity.Warning]).toBe(0)
})
