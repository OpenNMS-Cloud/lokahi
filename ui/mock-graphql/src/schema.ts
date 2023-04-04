import { makeExecutableSchema } from '@graphql-tools/schema'
import typeDefs from './typeDefs'
import { listAzureDiscoveries } from './data/discovery'
import { alertsList } from './data/alerts'

const resolvers = {
  Query: {
    listAzureDiscoveries: () => listAzureDiscoveries,
    alertsList: () => alertsList
  }
}

const schema = makeExecutableSchema({
  resolvers: [resolvers],
  typeDefs: [typeDefs]
})

export default {
  schema
}
