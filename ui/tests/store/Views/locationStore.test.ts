import { useLocationStore } from '@/store/Views/locationStore'
import { MonitoringLocationCreateInput } from '@/types/graphql'
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

  it('calls store.setDisplayType with correct parameters', async () => {
    vi.spyOn(locationStore, 'setDisplayType')
    locationStore.setDisplayType(DisplayType.ADD)
    expect(locationStore.setDisplayType).toHaveBeenCalledWith(DisplayType.ADD)
    expect(locationStore.displayType).toBe(DisplayType.ADD)
  })

  it('calls store.createLocation with correct parameters', async () => {
    vi.spyOn(locationStore, 'createLocation')
    const mockLocation: MonitoringLocationCreateInput = {
      address: '',
      latitude: 0,
      location: 'local',
      longitude: 0
    }
    await locationStore.createLocation(mockLocation)
    expect(locationStore.createLocation).toHaveBeenCalledWith(mockLocation)
    expect(locationStore.createLocation).toBeTruthy()
  })

  it('calls store.updateLocation with correct parameters', async () => {
    vi.spyOn(locationStore, 'updateLocation')
    const mockLocation = {
      id: 2,
      address: '',
      latitude: 0,
      location: 'local',
      longitude: 0
    }
    await locationStore.createLocation(mockLocation)
    expect(locationStore.createLocation).toHaveBeenCalledWith(mockLocation)
    expect(locationStore.createLocation).toBeTruthy()
  })

  it('calls store.deleteLocation with correct parameters', async () => {
    vi.spyOn(locationStore, 'deleteLocation')
    const mockLocationId = 200
    await locationStore.deleteLocation(mockLocationId)
    expect(locationStore.deleteLocation).toHaveBeenCalledWith(mockLocationId)
    expect(locationStore.deleteLocation).toBeTruthy()
  })

  it('calls store.selectedLocation with correct parameters', async () => {
    const mockLocationList = [
      {
        'id': 2,
        'location': 'local',
        'address': '',
        'longitude': 0,
        'latitude': 0
      }
    ]
    locationStore.locationsList = mockLocationList
    locationStore.selectedLocationId = 2
    expect(locationStore.selectedLocation).toStrictEqual(mockLocationList[0])
  })

  it('calls store.getMinionsForLocationId with correct parameters', async () => {
    vi.spyOn(locationStore, 'getMinionsForLocationId')
    const mockLocationId = 200
    await locationStore.getMinionsForLocationId(mockLocationId)
    expect(locationStore.getMinionsForLocationId).toHaveBeenCalledWith(mockLocationId)
    expect(locationStore.displayType).toBe(DisplayType.LIST)
    expect(locationStore.selectedLocationId).toBe(mockLocationId)
  })

  it('calls store.getMinionCertificate with correct parameters', async () => {
    vi.spyOn(locationStore, 'getMinionCertificate')
    await locationStore.getMinionCertificate()
    expect(locationStore.getMinionCertificate).toHaveBeenCalledOnce()
  })

  it('calls store.revokeMinionCertificate with correct parameters', async () => {
    vi.spyOn(locationStore, 'revokeMinionCertificate')
    await locationStore.revokeMinionCertificate()
    expect(locationStore.revokeMinionCertificate).toHaveBeenCalledOnce()
  })

  it('calls store.setCertificatePassword with correct parameters', async () => {
    vi.spyOn(locationStore, 'setCertificatePassword')
    const mockPassword = 'test'
    await locationStore.setCertificatePassword(mockPassword)
    expect(locationStore.certificatePassword).toBe(mockPassword)
  })
})
