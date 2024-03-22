import { createTestingPinia } from '@pinia/testing'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { eventsFixture } from '../../fixture/events'

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
            events: eventsFixture()
          }
        }
      }))
    }))

    const nodeStatusQueries = useNodeStatusQueries()
    nodeStatusQueries.setNodeId(1)

    const expectedFetchedData = {
      events: eventsFixture()
    }
    expect(nodeStatusQueries.fetchedEventsData).toStrictEqual(expectedFetchedData)
  })
})
