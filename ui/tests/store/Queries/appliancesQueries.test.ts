import { createTestingPinia } from '@pinia/testing'
import { useAppliancesQueries } from '@/store/Queries/appliancesQueries'
import { minionFixture } from '../../fixture/minions'
import { nodeFixture } from '../../fixture/nodes'
import {
  minionLatencyFixture,
  minionUptimeFixture,
  deviceLatencyFixture,
  deviceUptimeFixture
} from '../../fixture/metrics'
import { locationsFixture, expectedLocations } from '../../fixture/locations'

describe('Appliances queries', () => {
  beforeEach(() => {
    createTestingPinia()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // TODO: will fix after demo
  it.skip('fetched minions, nodes, metrics and locations', () => {
    vi.mock('villus', () => ({
      useQuery: vi.fn().mockImplementation(() => ({
        data: {
          value: {
            findAllMinions: [minionFixture()],
            minionLatency: minionLatencyFixture(),
            minionUptime: minionUptimeFixture(),
            findAllNodes: [nodeFixture()],
            deviceLatency: deviceLatencyFixture(),
            deviceUptime: deviceUptimeFixture(),
            findAllLocations: locationsFixture()
          }
        },
        isFetching: {
          value: false
        }
      }))
    }))

    const expectedAppliancesMinions = [
      {
        id: 'minion-01',
        label: 'minionlabel',
        lastCheckedTime: 1670542388,
        location: {
          location: 'default',
          id: 1
        },
        systemId: 'opennms-minion-8d6f5f64f-4l4wh',
        locationId: 1 // TODO: this should be removed once Minion type updated
      }
    ]

    const expectedAppliancesDevices = [
      {
        id: 1,
        nodeLabel: 'France',
        monitoringLocationId: 1,
        createTime: '2022-09-07T17:52:51Z'
      }
    ]

    const appliancesQueries = useAppliancesQueries()
    expect(appliancesQueries.tableMinions).toStrictEqual(expectedAppliancesMinions)
    expect(appliancesQueries.tableNodes).toStrictEqual(expectedAppliancesDevices)
    expect(appliancesQueries.locationsList).toStrictEqual(expectedLocations)
  })
})
