import { LocationDto, LocationCollectionDto } from '@/types/graphql'

const mockLocation: LocationDto = {
  'locationName': 'Default'
}
const locationsFixture = (props: Partial<LocationDto> = {}): LocationCollectionDto => ({
  locations: [
    { ...mockLocation, ...props }
  ]
})

const expectedLocations = [
  {
    'id': 0,
    'name': 'Default'
  }
]

export {
  locationsFixture,
  expectedLocations
}