import { Monitor } from '@/types'
import {
  DownloadMinionCertificateForWelcomeDocument,
  FindLocationsForWelcomeDocument,
  ListMinionsForTableDocument,
  ListNodeMetricsDocument,
  ListNodesForTableDocument,
  TimeRangeUnit
} from '@/types/graphql'
import { defineStore } from 'pinia'
import { useQuery } from 'villus'

export const useWelcomeQueries = defineStore('welcomeQueries', () => {
  const getAllMinions = async (): Promise<unknown[]> => {
    const { execute } = useQuery({
      query: ListMinionsForTableDocument,
      cachePolicy: 'network-only'
    })

    const allMinions = await execute()
    const rawResult = toRaw(allMinions.data)?.findAllMinions ?? []
    return allMinions.error ? [] : rawResult
  }

  const getLocationsForWelcome = async () => {
    const { execute } = useQuery({
      query: FindLocationsForWelcomeDocument,
      cachePolicy: 'network-only',
      fetchOnMount: false
    })

    const response = await execute()
    return toRaw(response?.data?.findAllLocations)?.map((d) => ({ id: d.id, location: d.location ?? '' })) ?? []
  }

  const getMinionCertificate = async (locationId: number) => {
    const { execute } = useQuery({
      query: DownloadMinionCertificateForWelcomeDocument,
      cachePolicy: 'network-only',
      fetchOnMount: false,
      variables: { location: locationId }
    })

    const cert = await execute()
    return { password: cert.data?.getMinionCertificate?.password, certificate: cert.data?.getMinionCertificate?.certificate }
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const getNodeDetails = async (name: string) => {
    const { execute: getDetails } = useQuery({
      query: ListNodesForTableDocument,
      cachePolicy: 'network-only',
      fetchOnMount: false
    })

    const details = await getDetails()
    const firstDetail = details?.data?.findAllNodes?.[0]
    let metrics

    if (firstDetail) {
      const { execute: getMetrics } = useQuery({
        query: ListNodeMetricsDocument,
        cachePolicy: 'network-only',
        fetchOnMount: false,
        variables: {
          id: firstDetail.id,
          instance: firstDetail.ipInterfaces?.[0].ipAddress ?? '',
          monitor: Monitor.ICMP, timeRange: 1, timeRangeUnit: TimeRangeUnit.Minute
        }
      })
      metrics = await getMetrics()
    }
    return { detail: firstDetail, metrics: metrics?.data }
  }

  return {
    getAllMinions,
    getLocationsForWelcome,
    getMinionCertificate,
    getNodeDetails
  }
})
