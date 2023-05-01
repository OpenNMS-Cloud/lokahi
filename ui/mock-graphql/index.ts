import { createServer } from 'node:http'
import { createYoga } from 'graphql-yoga'
import schema from './src/schema'

const start = async () => {
  const yoga = createYoga(schema)
  const server = createServer(yoga)
  server.listen(4000, () => {
    console.info('GQL Mock server is running on http://localhost:4000/graphql')
  })
}

start()
