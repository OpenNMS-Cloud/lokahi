import { defineStore } from 'pinia'
import { useLocationQueries } from '../Queries/locationQueries'
import { useMinionsQueries } from '../Queries/minionsQueries'
import { DisplayType } from '@/types/locations.d'
import { useLocationMutations } from '../Mutations/locationMutations'
import { MonitoringLocation, MonitoringLocationCreateInput, MonitoringLocationUpdateInput } from '@/types/graphql'

export const useLocationStore = defineStore('locationStore', () => {
  const locationsList = ref<MonitoringLocation[]>([])
  const selectedLocationId = ref()
  const certificatePassword = ref('')
  const displayType = ref(DisplayType.LIST)

  const saveIsFetching = ref()
  const updateIsFetching = ref()

  const locationQueries = useLocationQueries()
  const minionsQueries = useMinionsQueries()
  const locationMutations = useLocationMutations()

  const fetchLocations = async () => {
    try {
      const locations = await locationQueries.fetchLocations()

      locationsList.value = locations?.data?.value?.findAllLocations ?? []
    } catch (err) {
      locationsList.value = []
    }
  }

  const searchLocations = async (searchTerm = '') => {
    try {
      const locations = await locationQueries.searchLocation(searchTerm)

      locationsList.value = locations?.data?.value?.searchLocation ?? []
    } catch (err) {
      locationsList.value = []
    }
  }

  const selectLocation = (id: number | undefined) => {
    if (id) displayType.value = DisplayType.EDIT

    selectedLocationId.value = id
    certificatePassword.value = ''
  }

  const addLocation = () => {
    setDisplayType(DisplayType.ADD)
    selectedLocationId.value = undefined
  }

  const getMinionsForLocationId = (id: number | undefined) => {
    if (!id) return
    displayType.value = DisplayType.LIST
    selectedLocationId.value = id
    minionsQueries.findMinionsByLocationId(id)
  }

  const setDisplayType = (type: DisplayType) => {
    displayType.value = type
  }

  const createLocation = async (location: MonitoringLocationCreateInput) => {
    saveIsFetching.value = true

    const createdLocation = await locationMutations.createLocation(location)

    saveIsFetching.value = false

    if (!createdLocation.error) {
      await fetchLocations()
      selectedLocationId.value = createdLocation.data?.id
      setDisplayType(DisplayType.READY)
    }

    return !createdLocation.error
  }

  const updateLocation = async (location: MonitoringLocationUpdateInput) => {
    updateIsFetching.value = true

    const error = await locationMutations.updateLocation(location)

    updateIsFetching.value = false

    if (!error.value) {
      await fetchLocations()
    }

    return !error.value
  }

  const deleteLocation = async (id: number) => {
    displayType.value = DisplayType.LIST
    const error = await locationMutations.deleteLocation({ id })

    if (!error.value) {
      await fetchLocations()
    }

    return !error.value
  }

  const getMinionCertificate = async () => {
    if (!selectedLocation.value) return
    const response = await locationQueries.getMinionCertificate(selectedLocation.value.location)
    return response.data.value?.getMinionCertificate
  }

  const setCertificatePassword = (password: string) => {
    certificatePassword.value = password
  }

  const selectedLocation = computed(() => locationsList.value.filter((loc) => loc.id === selectedLocationId.value)[0] as Required<MonitoringLocation>)

  return {
    displayType,
    setDisplayType,
    locationsList,
    fetchLocations,
    selectedLocationId,
    selectLocation,
    searchLocations,
    createLocation,
    saveIsFetching,
    updateLocation,
    updateIsFetching,
    deleteLocation,
    getMinionCertificate,
    certificatePassword,
    setCertificatePassword,
    getMinionsForLocationId,
    addLocation,
    selectedLocation
  }
})
