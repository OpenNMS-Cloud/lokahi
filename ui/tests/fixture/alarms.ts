import { AlarmDto } from '@/types/graphql'

const mockAlarm: AlarmDto = {
  'id': 1,
  'uei': 'uei.opennms.org/provisioner/provisioningAdapterFailed',
  'severity': 'MAJOR',
  'ackTime': null,
  'firstAutomationTime': null
}
const alarmsFixture = (props: Partial<AlarmDto> = {}): AlarmDto[] => [
  { ...mockAlarm, ...props }
]

export {
  alarmsFixture
}