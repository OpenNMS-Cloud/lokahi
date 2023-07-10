import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import { setActiveClient, useClient } from 'villus'
import { createTestingPinia } from '@pinia/testing'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'

describe('Node Status Store', () => {
  beforeEach(() => {
    createTestingPinia({ stubActions: false })
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('Correctly calls the fetch exports query', async () => {
    setActiveClient(useClient({ url: 'http://test/graphql' }))

    const store = useNodeStatusStore()
    const queries = useNodeStatusQueries()
    await store.fetchExporters(1)

    expect(queries.fetchExporters).toHaveBeenCalledOnce()
    expect(queries.fetchExporters).toHaveBeenCalledWith({
      exporter: [
        {
          nodeId: 1
        }
      ]
    })
  })
})
