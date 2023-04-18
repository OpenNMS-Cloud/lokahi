import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import {
  FindApplicationsDocument,
  FindApplicationSeriesDocument,
  FindApplicationSummariesDocument,
  FindExportersDocument,
  RequestCriteriaInput
} from '@/types/graphql'

export const useflowsQueries = defineStore('flowsQueries', {
  state: () => {
    const getApplicationsSeries = async (requestCriteria: RequestCriteriaInput) => {
      const { execute, data } = useQuery({
        query: FindApplicationSeriesDocument,
        variables: {
          requestCriteria
        },
        cachePolicy: 'network-only'
      })
      await execute()
      return data
    }
    const getApplications = async (requestCriteria: RequestCriteriaInput) => {
      const { execute, data } = useQuery({
        query: FindApplicationsDocument,
        variables: {
          requestCriteria
        },
        cachePolicy: 'network-only'
      })
      await execute()
      return data
    }
    const getApplicationsSummaries = async (requestCriteria: RequestCriteriaInput) => {
      const { execute, data } = useQuery({
        query: FindApplicationSummariesDocument,
        variables: {
          requestCriteria
        },
        cachePolicy: 'network-only'
      })
      await execute()
      return data
    }
    const getExporters = async (requestCriteria: RequestCriteriaInput) => {
      const { execute, data } = useQuery({
        query: FindExportersDocument,
        variables: {
          requestCriteria
        },
        cachePolicy: 'network-only'
      })
      await execute()
      return data
    }

    return {
      getApplicationsSeries,
      getApplications,
      getApplicationsSummaries,
      getExporters
    }
  }
})
