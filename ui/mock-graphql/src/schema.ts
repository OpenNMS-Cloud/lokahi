import { makeExecutableSchema } from '@graphql-tools/schema'
import typeDefs from './typeDefs'
import { minion, listMinions } from './data/minions'

const resolvers = {
  Query: {
    minion: () => minion,
    listMinions: () => listMinions
  },
  Mutation: {
    saveRoutingKey: (_: any, { key }) => key,
  }
}

const schema = makeExecutableSchema({
  resolvers: [resolvers],
  typeDefs: [typeDefs]
})

export default {
  schema
}
