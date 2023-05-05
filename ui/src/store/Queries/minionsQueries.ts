import { defineStore } from 'pinia'
import { useQuery } from 'villus'
import { ListMinionsForTableDocument, ListMinionMetricsDocument, Minion, TimeRangeUnit } from '@/types/graphql'
import { ExtendedMinion } from '@/types/minion'
import useSpinner from '@/composables/useSpinner'
import { Monitor } from '@/types'

export const useMinionsQueries = defineStore('minionsQueries', () => {
  const minionsList = ref<ExtendedMinion[]>([])

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
        const [, val] = result as number[]

        minionsList.value.push({
          ...minion,
          latency: {
            value: val
          }
        })
      } else minionsList.value.push(minion)
    })
  }

  return { minionsList: computed(() => minionsList.value), fetchMinions }
})
