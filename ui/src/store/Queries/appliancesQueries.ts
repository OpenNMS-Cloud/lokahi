import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import {
  ListNodesForTableDocument,
  ListMinionsForTableDocument,
  ListMinionsAndDevicesForTablesDocument,
  ListMinionMetricsDocument,
  ListNodeMetricsDocument,
  TsResult,
  Minion,
  Node,
  TimeRangeUnit
} from '@/types/graphql'
import { ExtendedMinion } from '@/types/minion'
import { ExtendedNode } from '@/types/node'
import useSpinner from '@/composables/useSpinner'
import { Monitor } from '@/types'

export const useAppliancesQueries = defineStore('appliancesQueries', {
  state: () => {
    const tableMinions = ref<ExtendedMinion[]>([])
    const tableNodes = ref<ExtendedNode[]>([])

    const { startSpinner, stopSpinner } = useSpinner()

    const fetchMinionsForTable = () => {
      const { data: minionsData, isFetching } = useQuery({
        query: ListMinionsForTableDocument,
        cachePolicy: 'network-only'
      })

      watchEffect(() => {
        isFetching.value ? startSpinner() : stopSpinner()

        const allMinions = minionsData.value?.findAllMinions as Minion[]
        if (allMinions?.length) {
          addMetricsToMinions(allMinions)
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
        const { data, isFetching } = await fetchMinionMetrics(minion.systemId as string)
        const minionLatency = data.value?.minionLatency?.data?.result as TsResult[]

        if (!isFetching.value) {
          if (minionLatency?.length) {
            const [...values] = [...minionLatency][0].values as number[][]

            tableMinions.value.push({
              ...minion,
              latency: {
                value: values[values.length - 1][1] // get the last item of the list
              }
            })
          } else tableMinions.value.push(minion)
        }
      })
    }

    const fetchNodesForTable = () => {
      const { data: nodesData, isFetching } = useQuery({
        query: ListNodesForTableDocument,
        cachePolicy: 'network-only'
      })

      watchEffect(() => {
        isFetching.value ? startSpinner() : stopSpinner()

        const allNodes = nodesData.value?.findAllNodes as Node[]
        if (allNodes?.length) {
          addMetricsToNodes(allNodes)
        }
      })
    }

    const fetchNodeMetrics = (id: number, instance: string) =>
      useQuery({
        query: ListNodeMetricsDocument,
        variables: {
          id,
          instance,
          monitor: Monitor.ICMP,
          timeRange: 1,
          timeRangeUnit: TimeRangeUnit.Minute
        },
        cachePolicy: 'network-only'
      })

    const addMetricsToNodes = (allNodes: Node[]) => {
      tableNodes.value = []

      allNodes.forEach(async (node) => {
        const { data, isFetching } = await fetchNodeMetrics(
          node.id as number,
          node.ipInterfaces?.[0].ipAddress as string
        ) // currently only 1 interface per node
        const nodeLatency = data.value?.nodeLatency?.data?.result as TsResult[]
        const status = data.value?.nodeStatus?.status

        if (!isFetching.value) {
          let tableNode: ExtendedNode = {
            ...node,
            status
          }

          if (nodeLatency?.length) {
            const [...values] = [...nodeLatency][0].values as number[][]

            tableNode = {
              ...tableNode,
              latency: {
                value: values[values.length - 1][1] // get the last item of the list
              }
            }
          }

          tableNodes.value.push(tableNode)
        }
      })
    }

    // minions AND nodes table
    const {
      data: minionsAndNodes,
      execute,
      isFetching
    } = useQuery({
      query: ListMinionsAndDevicesForTablesDocument,
      cachePolicy: 'network-only'
    })

    watchEffect(() => {
      isFetching.value ? startSpinner() : stopSpinner()

      const allMinions = minionsAndNodes.value?.findAllMinions as Minion[]
      if (allMinions?.length) {
        addMetricsToMinions(allMinions)
      }

      const allNodes = minionsAndNodes.value?.findAllNodes as Node[]
      if (allNodes?.length) {
        addMetricsToNodes(allNodes)
      }
    })

    const locations = computed(() => minionsAndNodes.value?.findAllLocations || [])

    return {
      tableMinions,
      fetchMinionsForTable,
      tableNodes,
      fetchNodesForTable,
      locations,
      fetch: execute
    }
  }
})
