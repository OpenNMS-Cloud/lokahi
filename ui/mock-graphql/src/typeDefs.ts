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
  type Alert {
    id: String!
    name: String!
    severity: String!
    cause: String!
    duration: String!
    nodeType: String!
    date: String!
    time: String!
    isAcknowledged: Boolean!
    description: String!
    isSelected: Boolean
  }
  type Query {
    listAzureDiscoveries: [AzureDiscovery!]!
    alertsList: [Alert!]!
  }
`
export default typeDefs
