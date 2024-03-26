import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import {
  EventType,
  ListAlertEventDefinitionsDocument
} from '@/types/graphql'

export const useAlertEventDefinitionQueries = defineStore('alertEventDefinitionQueries', () => {
  const listAlertEventDefinitions = async (eventType: EventType) => {
    const { data, execute } = useQuery({
      query: ListAlertEventDefinitionsDocument,
      variables: {
        eventType: eventType
      },
      fetchOnMount: false,
      cachePolicy: 'network-only'
    })

    await execute()

    console.log(data, '111111111111111111111111111111')


    return data
  }

  return {
    listAlertEventDefinitions
  }
})
