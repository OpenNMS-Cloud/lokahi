import { createTestingPinia } from '@pinia/testing'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { eventsFixture } from '../../fixture/events'
import { nodeFixture } from '../../fixture/nodes'

describe('Events queries', () => {
  beforeEach(() => {
    createTestingPinia()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('fetched events, nodes and metrics', () => {
    vi.mock('villus', () => ({
      useQuery: vi.fn().mockImplementation(() => ({
        data: {
          value: {
            events: eventsFixture(),
            node: { details: nodeFixture() }
          }
        }
      }))
    }))

    const nodeStatusQueries = useNodeStatusQueries()
    nodeStatusQueries.setNodeId(2)

    const expectedFetchedData = {
      events: eventsFixture(),
      node: nodeFixture()
    }
    expect(nodeStatusQueries.fetchedData).toStrictEqual(expectedFetchedData)
  })
})
