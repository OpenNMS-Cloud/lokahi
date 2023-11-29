import { Severity } from '@/types/graphql'

export const Severities: Severity[] = [
  Severity.Critical,
  Severity.Major,
  Severity.Minor,
  Severity.Warning
]

export const Ack = 'Acknowledged'
export const UnAck = 'Unacknowledged'

export const Statuses = [Ack, UnAck]
