import { defineStore } from 'pinia'
import { useMutation } from 'villus'
import { CreateEventDocument } from '@/types/graphql'

export const useEventMutations = defineStore('eventMutations', () => {
  const {
    execute: createEvent
  } = useMutation(CreateEventDocument)

  return {
    createEvent
  }
})
