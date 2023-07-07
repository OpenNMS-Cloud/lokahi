import {defineStore} from 'pinia'
import {useMutation} from 'villus'
import {
  CreateLocationDocument,
  UpdateLocationDocument,
  DeleteLocationDocument,
  RevokeMinionCertificateDocument,
  MonitoringLocationCreateInput,
  MonitoringLocationUpdateInput
} from '@/types/graphql'

export const useLocationMutations = defineStore('locationMutations', () => {
  const createLocation = async (location: MonitoringLocationCreateInput) => {
    const {execute: execute, error, data} = useMutation(CreateLocationDocument)

    if (typeof location.latitude !== 'number') {
      location.latitude = 0
    }
    if (typeof location.longitude !== 'number') {
      location.longitude = 0
    }
    await execute({'location': location})

    return { error: error.value, data: data.value?.createLocation }
  }

  const updateLocation = async (location: MonitoringLocationUpdateInput) => {
    const { execute, error } = useMutation(UpdateLocationDocument)

    if (typeof location.latitude !== 'number') {
      location.latitude = 0
    }
    if (typeof location.longitude !== 'number') {
      location.longitude = 0
    }
    await execute({'location': location})

    return error
  }

  const deleteLocation = async (payload: { id: number }) => {
    const { execute, error } = useMutation(DeleteLocationDocument)

    await execute(payload)

    return error
  }

  const revokeMinionCertificate = async (payload: { locationId: number }) => {
    const { execute, error } = useMutation(RevokeMinionCertificateDocument)

    await execute({ locationId: payload })

    return error
  }

  return { createLocation, updateLocation, deleteLocation, revokeMinionCertificate }
})
