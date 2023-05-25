import {defineStore} from 'pinia'
import {useMutation} from 'villus'
import {
  CreateLocationDocument,
  UpdateLocationDocument,
  DeleteLocationDocument,
  MonitoringLocationCreateInput,
  MonitoringLocationUpdateInput
} from '@/types/graphql'

export const useLocationMutations = defineStore('locationMutations', () => {
  const createLocation = async (location: MonitoringLocationCreateInput) => {
    const {execute: execute, error} = useMutation(CreateLocationDocument)

    await execute({'location': location})

    return error
  }

  const updateLocation = async (location: MonitoringLocationUpdateInput) => {
    const { execute, error } = useMutation(UpdateLocationDocument)

    await execute({'location': location})

    return error
  }

  const deleteLocation = async (payload: { id: number}) => {
    const { execute, error } = useMutation(DeleteLocationDocument)

    await execute(payload)

    return error
  }

  return { createLocation, updateLocation, deleteLocation }
})
