import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import {
  ListNodesForTableDocument,
  ListMinionsForTableDocument,
  ListMinionsAndDevicesForTablesDocument,
  ListMinionMetricsDocument,
  ListNodeMetricsDocument,
  Minion,
  Node,
  TimeRangeUnit
} from '@/types/graphql'
import { ExtendedMinion } from '@/types/minion'
import { ExtendedNode } from '@/types/node'
import useSpinner from '@/composables/useSpinner'
import { AZURE_SCAN, Monitor } from '@/types'

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
        } else {
          tableMinions.value = []
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

          tableMinions.value.push({
            ...minion,
            latency: {
              value: val
            }
          })
        } else tableMinions.value.push(minion)
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
        const { ipAddress: snmpPrimaryIpAddress } = node.ipInterfaces?.filter((ii) => ii.snmpPrimary)[0] || {} // not getting ipAddress from snmpPrimary interface can result in missing metrics for ICMP
        const instance = node.scanType === AZURE_SCAN ? `azure-node-${node.id}` : snmpPrimaryIpAddress!
        const { data, isFetching } = await fetchNodeMetrics(node.id as number, instance)
        const latencyResult = data.value?.nodeLatency?.data?.result?.[0]?.values?.[0]
        const status = data.value?.nodeStatus?.status

        if (!isFetching.value) {
          let tableNode: ExtendedNode = {
            ...node,
            status
          }

          if (latencyResult) {
            const [, val] = latencyResult as number[]

            tableNode = {
              ...tableNode,
              latency: {
                value: val
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

    watch(isFetching, (_, fetched) => {
      fetched ? stopSpinner() : startSpinner()
    })

    watchEffect(() => {
      const allMinions = minionsAndNodes.value?.findAllMinions as Minion[]
      if (allMinions?.length) {
        addMetricsToMinions(allMinions)
      }

      const allNodes = minionsAndNodes.value?.findAllNodes as Node[]
      if (allNodes?.length) {
        addMetricsToNodes(allNodes)
      }
    })

    const locationsList = computed(() => minionsAndNodes.value?.findAllLocations ?? [])

    return {
      tableMinions,
      fetchMinionsForTable,
      tableNodes,
      fetchNodesForTable,
      locationsList,
      fetch: execute
    }
  }
})
