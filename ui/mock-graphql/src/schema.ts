import { makeExecutableSchema } from '@graphql-tools/schema'
import typeDefs from './typeDefs'
import { listAzureDiscoveries } from './data/discovery'

const resolvers = {
  Query: {
    listAzureDiscoveries: () => listAzureDiscoveries
  }
}

const schema = makeExecutableSchema({
  resolvers: [resolvers],
  typeDefs: [typeDefs]
})

export default {
  schema
}
