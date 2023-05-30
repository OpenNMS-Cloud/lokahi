import { Minion } from '@/types/graphql'

const mockData: Minion = {
  id: 'minion-01',
  label: 'minionlabel',
  lastCheckedTime: 1670542388,
  location: {
    location: 'default',
    id: 1,
    address: 'address',
    latitude: 0.0,
    longitude: 0.0
  },
  systemId: 'opennms-minion-8d6f5f64f-4l4wh',
  locationId: 1 // TODO: this should be removed once Minion type updated
}

export const minionFixture = (mockMinion = mockData, props: Partial<Minion> = {}): Minion => ({
  ...mockMinion,
  ...props
})
