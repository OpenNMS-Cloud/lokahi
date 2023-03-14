export interface SearchResult {
  identifier: string
  label: string
  matches: any
  properties: any
  url: string
  weight: number
}

export interface SearchResultResponse {
  label?: string
  context: {
    name: string
    weight: number
  }
  empty: boolean
  more: boolean
  results: SearchResult[]
}
