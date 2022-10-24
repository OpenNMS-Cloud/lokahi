import { EventDto, EventCollectionDto } from '@/types/graphql'

const mockData: EventDto = {
  'id': 2,
  'severity': 'WARNING',
  'time': '2022-09-20T09:25:44Z',
  'source': 'Device-Rest-Service',
  'nodeLabel': 'Unknown',
  'location': 'Default',
  'ipAddress': '',
  'nodeId': 1
}
const eventsFixture = (props: Partial<EventDto> = {}): EventCollectionDto => ({
  events: [
    { ...mockData, ...props }
  ],
  offset: 0,
  totalCount: 1
})

export {
  eventsFixture
}