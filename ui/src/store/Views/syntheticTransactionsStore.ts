import { defineStore } from 'pinia'

// eslint-disable-next-line @typescript-eslint/ban-types
type TState = {}

export const useSyntheticTransactionsStore = defineStore('syntheticTransactionsStore', {
  state: (): TState => ({}),
  actions: {}
})
