interface IKey {
  [key: string]: any
}

export interface DiscoveryInput extends IKey {
  id: number | null
  type: number
  name: string
  location: string[]
  tags: string[]
  IPRange: string
  communityString?: string | null
  UDPPort?: number | string | null
}
