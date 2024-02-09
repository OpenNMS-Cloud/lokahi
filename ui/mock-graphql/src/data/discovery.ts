// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-nocheck
import casual from 'casual'

casual.define('azureDiscovery', function () {
  return {
    id: casual.uuid,
    name: `Azure-${casual.word}`,
    clientId: casual.uuid,
    subscriptionId: casual.uuid,
    directoryId: casual.uuid,
    location: { location: 'Default', id: casual.uuid },
    tags: ['tag1']
  }
})

casual.define('listAzureDiscoveries', function () {
  return [casual.azureDiscovery]
})

const listAzureDiscoveries = casual.listAzureDiscoveries

export { listAzureDiscoveries }
