import { MonitoringLocation } from '@/types/graphql'

const mockLocation: MonitoringLocation = {
  location: 'Default',
  id: 1,
  address: 'address',
  latitude: 0.0,
  longitude: 0.0 
}

const locationsFixture = (props: Partial<MonitoringLocation> = {}): MonitoringLocation[] => [{ ...mockLocation, ...props }]

const expectedLocations = [
  {
    id: 1,
    location: 'Default',
    address: 'address',
    latitude: 0.0,
    longitude: 0.0 
  }
]
export { locationsFixture, expectedLocations }
