import { useQuery } from 'villus'
import { defineStore } from 'pinia'
import {
  BuildNetworkInventoryPageDocument
} from '@/types/graphql'

export const useInventoryQueries = defineStore('inventoryQueries', () => {

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
    buildNetworkInventory
  }
})
