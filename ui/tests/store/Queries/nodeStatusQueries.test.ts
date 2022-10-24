import { createTestingPinia } from '@pinia/testing'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { eventsFixture } from '../../fixture/events'
import { deviceFixture } from '../../fixture/devices'
import { deviceLatencyFixture, deviceUptimeFixture } from '../../fixture/metrics'

describe('Events queries', () => {
  beforeEach(() => {
    createTestingPinia()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('fetched events, devices and metrics', () => {
    vi.mock('villus', () => ({
      useQuery: vi.fn().mockImplementation(() => ({
        data: { 
          value: { 
            listEvents: eventsFixture(),
            device: deviceFixture(),
            deviceLatency: deviceLatencyFixture(),
            deviceUptime: deviceUptimeFixture()
          }}
      }))
    }))
    
    const nodeStatusQueries = useNodeStatusQueries()
    nodeStatusQueries.setNodeId(2)

    const expectedFetchedData = {
      listEvents: eventsFixture(),
      device: deviceFixture(),
      deviceLatency: (deviceLatencyFixture()).data?.result,
      deviceUptime: (deviceUptimeFixture()).data?.result
    }
    expect(nodeStatusQueries.fetchedData).toStrictEqual(expectedFetchedData)
  })
})