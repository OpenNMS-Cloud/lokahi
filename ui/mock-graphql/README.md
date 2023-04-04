This project serves as a GraphQL mock server.

- It can run a local server (nodemon), with mocked queries if needed.
- It can automate and generate typing of GraphQL operations (codegen) from .graphql files (querying purposes)

## Installation

```
yarn install
```

## Mock server

```
yarn serve
```

Serves at: `http://127.0.0.1:4000/graphql`

## Typing generation

```
yarn generate
```

Note: required mock server be running

## Config

.env.development.local

```
VITE_BASE_URL=http://127.0.0.1:4000/api # GraphQL mock server (./mock-graphql)
```

## Usage example

```
import { useQuery } from 'villus'
import { AlertListDocument } from '@/types/graphql-mocks'

const { data: fetchAlertsData, execute: fetchAlerts } = useQuery({
  query: AlertListDocument,
  fetchOnMount: false,
  cachePolicy: 'network-only'
})
```

alerts.ts

```
import casual from 'casual'
import { rndSeverity, rndDuration, rndNodeType } from '../helpers/random'

casual.define('alert', function () {
  return {
    id: casual.uuid,
    name: `Alert-${casual.word}-${casual.word}-${casual.word}-${casual.word}`,
    severity: rndSeverity(),
    cause: casual.random_element(['Power supply failure', casual.catch_phrase, casual.catch_phrase]),
    duration: rndDuration(),
    nodeType: rndNodeType(),
    date: '9999-99-99',
    time: '00:00:00',
    isAcknowledged: casual.random_element([true, false]),
    description: casual.description
  }
})

casual.define('alertList', function () {
  return [casual.alert, casual.alert, casual.alert, casual.alert, casual.alert]
})

const alert = casual.alert
const alertList = casual.alertList

export { alert, alertList }
```

alertList.gql

```
query alertList {
  alertList {
    id
    name
    severity
    cause
    duration
    nodeType
    date
    time
    isAcknowledged
    description
  }
}
```

schema.ts

```
import { alertList } from './data/alerts'

const resolvers = {
  Query: {
    listAzureDiscoveries: () => listAzureDiscoveries,
    alertList: () => alertList
  }
}
...
```

typeDefs.ts

```
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
```
