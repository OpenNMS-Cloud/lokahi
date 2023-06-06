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
  const minionLocationId = reactive({ locationId: 0 })

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

  const addMetricsToMinions = (allMinions: Minion[]) => {
    allMinions.forEach(async (minion) => {
      const { data } = await fetchMinionMetrics(minion.systemId as string)
      const result = data.value?.minionLatency?.data?.result?.[0]?.values?.[0]

      if (result) {
        const [, val] = result

        minionsList.value.push({
          ...minion,
          latency: {
            value: val
          }
        })
      } else minionsList.value.push(minion)
    })
  }

  // find minions by location id
  const { onData: onFindMinionsByLocationId, isFetching: isFetchingMinionsByLocationId } = useQuery({
    query: FindMinionsByLocationIdDocument,
    cachePolicy: 'network-only',
    fetchOnMount: false,
    variables: minionLocationId
  })

  const findMinionsByLocationId = (locationId: number) => (minionLocationId.locationId = locationId)
  
  watchEffect(() => (isFetchingMinionsByLocationId.value ? startSpinner() : stopSpinner()))

  onFindMinionsByLocationId((data) => {
    if (data.findMinionsByLocationId?.length) {
      addMetricsToMinions(data.findMinionsByLocationId as Minion[])
    } else {
      minionsList.value = []
    }
  })

  return {
    minionsList: computed(() => minionsList.value),
    fetchMinions,
    findMinionsByLocationId
  }
})
