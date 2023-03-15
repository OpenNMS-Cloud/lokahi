const typeDefs = `
  type Location {
    id: String!
    location: String!
  }
  type AzureDiscovery {
    id: String!
    name: String!
    clientId: String!
    subscriptionId: String!
    directoryId: String!
    location: Location!
    tags: [String]!
  }
  type Query {
    listAzureDiscoveries: [AzureDiscovery!]!
  }
`
export default typeDefs
