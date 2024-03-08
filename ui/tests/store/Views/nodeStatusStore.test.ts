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

  // Skipping this test until flows are fully enabled
  it.skip('Correctly calls the fetch exports query', async () => {
    setActiveClient(useClient({ url: 'http://test/graphql' }))

    const store = useNodeStatusStore()
    const queries = useNodeStatusQueries()

    const capturedNowInMs = new Date().getTime()
    const testDate = new Date(capturedNowInMs)

    global.Date = vitest.fn().mockImplementation(() => testDate) as any
    global.Date.now = vitest.fn().mockImplementation(() => capturedNowInMs)

    const endTime = Date.now()
    const startTime = endTime - 1000 * 60 * 60 * 24 * 7 // endTime - 7 days

    await store.fetchExporters(1)

    expect(queries.fetchExporters).toHaveBeenCalledOnce()

    expect(queries.fetchExporters).toHaveBeenCalledWith({
      exporter: [
        {
          nodeId: 1
        }
      ],
      timeRange: {
        startTime,
        endTime
      }
    })
  })
})
