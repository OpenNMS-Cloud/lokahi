import { Location } from '@/types/graphql'

const mockLocation: Location = {
  location: 'Default',
  id: 1
}
const locationsFixture = (props: Partial<Location> = {}): Location[] => ([{ ...mockLocation, ...props }])

const expectedLocations = [
  {
    'id': 1,
    'location': 'Default'
  }
]

export {
  locationsFixture,
  expectedLocations
}