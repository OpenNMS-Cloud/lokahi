import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import { setActiveClient, useClient } from 'villus'
import { createTestingPinia } from '@pinia/testing'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { useNodeMutations } from '@/store/Mutations/nodeMutations'

describe('Node Status Store', () => {
  beforeEach(() => {
    createTestingPinia({ stubActions: false })
    setActiveClient(useClient({ url: 'http://test/graphql' })) // Create and set a client
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


  it('calls nodeStatusQueries.setNodeId with correct parameter and sets nodeId', async () => {
    const store = useNodeStatusStore()
    const queries = useNodeStatusQueries()
    const mockNodeId = 123
    vi.spyOn(queries, 'setNodeId')
    await store.setNodeId(mockNodeId)
    expect(queries.setNodeId).toHaveBeenCalledWith(mockNodeId)
    expect(store.nodeId).toBe(mockNodeId)
  })


  it('calls mutations.updateNode with correct parameters', async () => {
    const store = useNodeStatusStore()
    const mutations = useNodeMutations()
    const mockNodeAlias = 'New Node Alias'
    vi.spyOn(mutations, 'updateNode')
    await store.updateNodeAlias(mockNodeAlias)
    expect(mutations.updateNode).toHaveBeenCalledWith({
      node: {
        nodeAlias: mockNodeAlias
      }
    })
  })
})
