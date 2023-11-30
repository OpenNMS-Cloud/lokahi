import { Severity } from '@/types/graphql'

export const Severities: Severity[] = [Severity.Critical, Severity.Major, Severity.Minor, Severity.Warning]

export const Ack = 'Acknowledged'
export const UnAck = 'Unacknowledged'

export const Statuses = [Ack, UnAck]

export const DefaultCountMap = {
  [Severity.Critical]: 0,
  [Severity.Major]: 0,
  [Severity.Minor]: 0,
  [Severity.Warning]: 0,
  [Ack]: 0,
  [UnAck]: 0
}
