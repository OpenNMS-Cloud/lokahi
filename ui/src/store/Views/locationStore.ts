import { defineStore } from 'pinia'
import { useLocationQueries } from '../Queries/locationQueries'
import { useMinionsQueries } from '../Queries/minionsQueries'
import { DisplayType } from '@/types/locations.d'
import { useLocationMutations } from '../Mutations/locationMutations'
import { MonitoringLocation, MonitoringLocationCreateInput, MonitoringLocationUpdateInput } from '@/types/graphql'
import useMinionCmd from '@/composables/useMinionCmd'

export const useLocationStore = defineStore('locationStore', () => {
  const locationsList = ref<MonitoringLocation[]>([])
  const selectedLocationId = ref()
  const certificatePassword = ref('')
  const displayType = ref(DisplayType.LIST)

  const saveIsFetching = ref()
  const updateIsFetching = ref()
  const certIsFetching = ref()

  const locationQueries = useLocationQueries()
  const minionsQueries = useMinionsQueries()
  const locationMutations = useLocationMutations()
  const { minionDockerCmd, setPassword, setMinionId, clearMinionCmdVals } = useMinionCmd()

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
    clearMinionCmdVals()
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
    certificatePassword.value = ''
    clearMinionCmdVals()
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
    certIsFetching.value = true
    const response = await locationQueries.getMinionCertificate(selectedLocation.value.id)
    certIsFetching.value = false
    return response.data.value?.getMinionCertificate
  }

  const revokeMinionCertificate = async () => {
    if (!selectedLocation.value) return
    const response = await locationMutations.revokeMinionCertificate(selectedLocation.value.id)
    if(!response.value){
      setCertificatePassword('')
    }

    return !response.value
  }

  const setCertificatePassword = (password: string) => {
    certificatePassword.value = password
    setPassword(certificatePassword.value)
    setMinionId(selectedLocation.value.location)
  }

  const selectedLocation = computed(
    () => locationsList.value.filter((loc) => loc.id === selectedLocationId.value)[0] as Required<MonitoringLocation>
  )

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
    certIsFetching,
    deleteLocation,
    getMinionCertificate,
    revokeMinionCertificate,
    certificatePassword,
    setCertificatePassword,
    getMinionsForLocationId,
    addLocation,
    selectedLocation,
    minionDockerCmd
  }
})
