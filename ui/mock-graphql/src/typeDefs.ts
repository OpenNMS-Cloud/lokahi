const typeDefs = `
  input LocationDTOInput {
    geolocation: String
    latitude: Float
    locationName: String
    longitude: Float
    monitoringArea: String
    priority: Int
    tags: [String]
  }
  type Minion {
    id: String!
    label: String!
    status: String!
    location: String!
    lastUpdated: String!
  }
  type ListMinions {
    minions: [Minion!]!
  }
  type Query {
    minion: Minion!
    listMinions: ListMinions!
  },
  type Mutation {
    saveRoutingKey(key: String!): String
  }
`

export default typeDefs
