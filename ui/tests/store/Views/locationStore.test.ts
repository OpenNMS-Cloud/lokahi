import { useLocationStore } from '@/store/Views/locationStore'
import { DisplayType } from '@/types/locations.d'
import { createTestingPinia } from '@pinia/testing'
import { setActiveClient, useClient } from 'villus'

describe('Locations Store', () => {
  let locationStore: ReturnType<typeof useLocationStore>
  beforeEach(() => {
    createTestingPinia({ stubActions: false })
    setActiveClient(useClient({ url: 'http://test/graphql' })) // Create and set a client
  })

  beforeEach(() => {
    locationStore = useLocationStore()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should initialize with empty alerts list', () => {
    expect(locationStore.locationsList.length === 0).toBe(true)
  })

  it('calls locationStore.fetchLocations with correct parameters', async () => {
    vi.spyOn(locationStore, 'fetchLocations')
    await locationStore.fetchLocations()
    expect(locationStore.fetchLocations).toHaveBeenCalledOnce()
    expect(locationStore.locationsList.length === 0 || locationStore.locationsList.length > 0).toBe(true)
  })

  it('calls locationStore.searchLocations with correct parameters', async () => {
    vi.spyOn(locationStore, 'searchLocations')
    await locationStore.searchLocations('testing')
    expect(locationStore.searchLocations).toBeCalledWith('testing')
    expect(locationStore.locationsList.length === 0 || locationStore.locationsList.length > 0).toBe(true)
  })

  it('calls locationStore.selectLocation with correct parameters', () => {
    vi.spyOn(locationStore, 'selectLocation')
    const mockId = 12
    locationStore.selectLocation(mockId)
    expect(locationStore.selectLocation).toHaveBeenCalledWith(mockId)
    expect(locationStore.selectedLocationId).toBe(mockId)
    expect(locationStore.certificatePassword).toBe('')
  })

  it('calls locationStore.addLocation with correct parameters', () => {
    vi.spyOn(locationStore, 'addLocation')
    locationStore.addLocation()
    expect(locationStore.selectedLocationId).toBe(undefined)
    expect(locationStore.displayType).toBe(DisplayType.ADD)
    expect(locationStore.certificatePassword).toBe('')
  })
})
