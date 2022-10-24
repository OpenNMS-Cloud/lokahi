import { createTestingPinia } from '@pinia/testing'
import { useAppliancesQueries } from '@/store/Queries/appliancesQueries'
import { minionFixture } from '../../fixture/minions'
import { deviceFixture } from '../../fixture/devices'
import { minionLatencyFixture, minionUptimeFixture, deviceLatencyFixture, deviceUptimeFixture } from '../../fixture/metrics'
import { locationsFixture, expectedLocations } from '../../fixture/locations'

describe('Appliances queries', () =>{
  beforeEach(() => {
    createTestingPinia()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('fetched minions, devices, metrics and locations', () => {
    vi.mock('villus', () => ({
      useQuery: vi.fn().mockImplementation(() => ({
        data: {
          value: {
            listMinions: {
              minions: [ minionFixture() ]
            },
            minionLatency: minionLatencyFixture(),
            minionUptime: minionUptimeFixture(),
            listDevices: {
              devices: [ deviceFixture() ]
            },
            deviceLatency: deviceLatencyFixture(),
            deviceUptime: deviceUptimeFixture(),
            listLocations: locationsFixture()
          }
        } 
      }))
    }))
    
    const expectedAppliancesMinions = [
      {
        id: 'minion-01',
        status: 'UP',
        location: 'Default',
        lastUpdated: '2022-09-09T12:17:09.497Z',
        icmp_latency: 2,
        snmp_uptime: 97419
      }
    ]

    const expectedAppliancesDevices = [
      {
        id: 1,
        label: 'France',
        createTime: '2022-09-07T17:52:51Z',
        managementIp: '127.0.0.1',
        icmp_latency: 0.17,
        snmp_uptime: undefined,
        status: 'DOWN'
      }
    ]

    const appliancesQueries = useAppliancesQueries()
    expect(appliancesQueries.tableMinions).toStrictEqual(expectedAppliancesMinions)
    expect(appliancesQueries.tableDevices).toStrictEqual(expectedAppliancesDevices)
    expect(appliancesQueries.locations).toStrictEqual(expectedLocations)
  })
})