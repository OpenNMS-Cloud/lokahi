import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import { LocationsListDocument, ListMinionsForTableDocument, SearchLocationDocument, GetMinionCertificateDocument, FindLocationsForWelcomeDocument } from '@/types/graphql'

export const useLocationQueries = defineStore('locationQueries', () => {
  const fetchLocations = () =>
    useQuery({
      query: LocationsListDocument,
      fetchOnMount: false,
      cachePolicy: 'network-only'
    })

  const fetchLocationsForWelcome = async () => {

    const { execute } = useQuery({
      query: FindLocationsForWelcomeDocument,
      cachePolicy: 'network-only',
      fetchOnMount: false,
    })
    const response = await execute();
    return toRaw(response?.data?.findAllLocations) || [];
  }

  const fetchMinions = () =>
    useQuery({
      query: ListMinionsForTableDocument,
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
    fetchLocationsForWelcome,
    fetchMinions,
    searchLocation,
    getMinionCertificate
  }
})
