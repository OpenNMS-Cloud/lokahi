import { defineStore } from 'pinia'

import { SearchResultResponse } from '@/types/search'

export interface State {
  searchResults: SearchResultResponse[]
}
export const useSearchStore = defineStore('searchStore', {
  state: () =>
    <State>{
      searchResults: []
    },
  actions: {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars 
    async search(searchStr: string) {
      // todo: add graphQL query
      const responses = [] as SearchResultResponse[]

      if (responses) {
        // add label and filter actions for dropdown display
        const results = responses.filter((resp) => {
          resp.label = resp.context.name
          if (resp.label !== 'Action') return resp
        })

        this.searchResults = results

        return results
      }

      return
    }
  }
})
