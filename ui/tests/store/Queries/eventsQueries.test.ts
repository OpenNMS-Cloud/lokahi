import { createTestingPinia } from '@pinia/testing'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { eventsFixture } from '../../fixture/events'
import { deviceFixture } from '../../fixture/devices'
import { latencyFixture, uptimeFixture } from '../../fixture/metrics'

describe('Events Queries', () => {
  beforeEach(() => {
    createTestingPinia()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('fetches events, devices and metrics', () => {
    vi.mock('villus', () => ({
      useQuery: vi.fn().mockImplementation(() => ({
        data: { 
          value: { 
            listEvents: eventsFixture(),
            device: deviceFixture(),
            deviceLatency: latencyFixture(),
            deviceUptime: uptimeFixture()
          }}
      }))
    }))
    
    const nodeStatusQueries = useNodeStatusQueries()
    nodeStatusQueries.setNodeId(2)

    const expectedFetchedData = {
      listEvents: { ...eventsFixture() },
      device: deviceFixture(),
      deviceLatency: (latencyFixture()).data?.result,
      deviceUptime: (uptimeFixture()).data?.result
    }
    expect(nodeStatusQueries.fetchedData).toStrictEqual(expectedFetchedData)
  })
})