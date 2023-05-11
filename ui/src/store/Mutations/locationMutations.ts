import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import { CreateLocationDocument, UpdateLocationDocument, DeleteLocationDocument, Location } from '@/types/graphql'

export const useLocationMutations = defineStore('locationMutations', () => {
  const createLocation = async (payload: Location) => {
    const { execute, error } = useMutation(CreateLocationDocument)

    await execute(payload) // TODO: api needs to save address/long/lat

    return error
  }

  const updateLocation = async (payload: {id: string, location: string}) => {
    const { execute, error } = useMutation(UpdateLocationDocument)

    await execute(payload) // TODO: api needs to save address/long/lat

    return error
  }

  const deleteLocation = async (payload: { id: number}) => {
    const { execute, error } = useMutation(DeleteLocationDocument)

    await execute(payload)

    return error
  }

  return { createLocation, updateLocation, deleteLocation }
})
