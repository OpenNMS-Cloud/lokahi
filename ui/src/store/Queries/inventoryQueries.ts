import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import {
  BuildNetworkInventoryPageDocument,
  FindAllNodesByTagsDocument,
  FindAllNodesByNodeLabelSearchDocument
} from '@/types/graphql'

export const useInventoryQueries = defineStore('inventoryQueries', () => {

  const getNodesByTags = async (tags: string | string[] | undefined) => {
    const { execute, data } = useQuery({
      query: FindAllNodesByTagsDocument,
      variables: {
        tags
      },
      cachePolicy: 'network-only'
    })
    await execute()
    return data
  }

  const getNodesByLabel = async (labelSearchTerm: string) => {
    const { execute, data } = useQuery({
      query: FindAllNodesByNodeLabelSearchDocument,
      variables: {
        labelSearchTerm
      },
      cachePolicy: 'network-only'
    })
    await execute()
    return data
  }

  const {
    onData: receivedNetworkInventory,
    isFetching: networkInventoryFetching,
    execute: buildNetworkInventory
  } = useQuery({
    query: BuildNetworkInventoryPageDocument,
    fetchOnMount: false,
    cachePolicy: 'network-only'
  })

  return {
    receivedNetworkInventory,
    networkInventoryFetching,
    buildNetworkInventory,
    getNodesByTags,
    getNodesByLabel
  }
})
