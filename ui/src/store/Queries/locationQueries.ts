import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import { LocationsListDocument, SearchLocationDocument, GetMinionCertificateDocument } from '@/types/graphql'

export const useLocationQueries = defineStore('locationQueries', () => {
  const fetchLocations = () =>
    useQuery({
      query: LocationsListDocument,
      fetchOnMount: false,
      cachePolicy: 'network-only'
    })

  const searchLocation = (searchTerm = '') =>
    useQuery({
      query: SearchLocationDocument,
      variables: {
        searchTerm
      },
      fetchOnMount: false,
      cachePolicy: 'network-only'
    })

  const getMinionCertificate = (location: string) =>
    useQuery({
      query: GetMinionCertificateDocument,
      variables: {
        location
      },
      fetchOnMount: false,
      cachePolicy: 'network-only'
    })

  return {
    fetchLocations,
    searchLocation,
    getMinionCertificate
  }
})
