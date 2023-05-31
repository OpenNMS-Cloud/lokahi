import { defineStore } from 'pinia'
import { useLocationQueries } from '../Queries/locationQueries'
import { useMinionsQueries } from '../Queries/minionsQueries'
import { DisplayType } from '@/types/locations.d'
import { useLocationMutations } from '../Mutations/locationMutations'
import { MonitoringLocationCreateInput, MonitoringLocationUpdateInput } from '@/types/graphql'

export const useLocationStore = defineStore('locationStore', () => {
  const downloadCertificatePassword = ref('')
  const locationsList = ref()
  const minionsList = ref()
  const selectedLocationId = ref()
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

  const fetchMinions = async () => {
    minionsQueries.fetchMinions()

    watchEffect(() => {
      minionsList.value = minionsQueries.minionsList
    })
  }

  const searchMinions = async (searchTerm = '') => {
    // const minions = await locationQueries.searchMinion(searchTerm)
    // minionsList.value = minions?.data?.value?.searchLocation || []
  }

  const selectLocation = (id: number | undefined) => {
    if (id) displayType.value = DisplayType.EDIT

    selectedLocationId.value = id
  }

  const setDisplayType = (type: DisplayType) => {
    displayType.value = type
  }

  const createLocation = async (location: MonitoringLocationCreateInput) => {
    saveIsFetching.value = true

    const error = await locationMutations.createLocation(location)

    saveIsFetching.value = false

    if (!error.value) {
      await fetchLocations()
    }

    return !error.value
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
    const error = await locationMutations.deleteLocation({ id })

    if (!error.value) {
      setDisplayType(DisplayType.LIST)
      await fetchLocations()
    }

    return !error.value
  }

  return {
    displayType,
    setDisplayType,
    locationsList,
    fetchLocations,
    selectedLocationId,
    selectLocation,
    searchLocations,
    minionsList,
    fetchMinions,
    searchMinions,
    createLocation,
    saveIsFetching,
    updateLocation,
    updateIsFetching,
    deleteLocation,
    downloadCertificatePassword
  }
})
