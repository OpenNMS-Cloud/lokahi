import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import {
  ListMinionsForTableDocument,
  ListMinionMetricsDocument,
  Minion,
  TimeRangeUnit,
  FindMinionsByLocationIdDocument
} from '@/types/graphql'
import { ExtendedMinion } from '@/types/minion'
import useSpinner from '@/composables/useSpinner'
import { Monitor } from '@/types'

export const useMinionsQueries = defineStore('minionsQueries', () => {
  const minionsList = ref<ExtendedMinion[]>([])
  const savedLocId = ref()

  const { startSpinner, stopSpinner } = useSpinner()

  const fetchMinions = () => {
    const { data: minionsData, isFetching } = useQuery({
      query: ListMinionsForTableDocument,
      cachePolicy: 'network-only'
    })

    watchEffect(() => {
      isFetching.value ? startSpinner() : stopSpinner()

      const allMinions = minionsData.value?.findAllMinions as Minion[]

      if (allMinions?.length) {
        addMetricsToMinions(allMinions)
      } else {
        minionsList.value = []
      }
    })
  }

  const fetchMinionMetrics = (instance: string) =>
    useQuery({
      query: ListMinionMetricsDocument,
      variables: {
        instance,
        monitor: Monitor.ECHO,
        timeRange: 1,
        timeRangeUnit: TimeRangeUnit.Minute
      },
      cachePolicy: 'network-only'
    })

  const addMetricsToMinions = async (allMinions: Minion[]) => {
    const updatedMinionsList = []
    for (const minion of allMinions) {
      const { data } = await fetchMinionMetrics(minion.systemId as string)
      const result = data.value?.minionLatency?.data?.result?.[0]?.values?.[0]

      if (result) {
        const [, val] = result
        updatedMinionsList.push({
          ...minion,
          latency: {
            value: val
          }
        })
      } else {
        updatedMinionsList.push(minion)
      }
    }
    minionsList.value = updatedMinionsList
  }

  const findMinionsByLocationId = async (locationId?: number) => {
    if (!locationId && !savedLocId.value) return
    if (locationId) savedLocId.value = locationId

    const { execute, data } = useQuery({
      query: FindMinionsByLocationIdDocument,
      variables: { locationId: savedLocId.value },
      cachePolicy: 'network-only',
      fetchOnMount: false
    })

    startSpinner()
    await execute()
    stopSpinner()

    if (data.value?.findMinionsByLocationId?.length) {
      addMetricsToMinions(data.value.findMinionsByLocationId as Minion[])
    } else {
      minionsList.value = []
    }

    return data.value?.findMinionsByLocationId
  }

  return {
    minionsList: computed(() => minionsList.value),
    fetchMinions,
    findMinionsByLocationId
  }
})
