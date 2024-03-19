import { Event } from '@/types/graphql'

const mockData: Event = {
  id: 2,
  uei: 'uei',
  nodeId: 1,
  ipAddress: '127.0.0.1',
  producedTime: '2022-09-20T09:25:44Z'
}

export const eventsFixture = (props: Partial<Event> = {}) => ([{ ...mockData, ...props }])
