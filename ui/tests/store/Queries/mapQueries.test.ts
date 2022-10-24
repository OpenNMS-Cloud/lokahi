import { createTestingPinia } from '@pinia/testing'
import { useMapQueries } from '@/store/Queries/mapQueries'
import { deviceFixture } from '../../fixture/devices'
import { DeviceDto } from '@/types/graphql'

const mockMapDevice: DeviceDto = {
  'foreignId': '',
  'foreignSource': '',
  'labelSource': '',
  'location': {
    'latitude': 46.69197463989258,
    'longitude': 2.377929925918579
  },
  'sysContact': '',
  'sysDescription': '',
  'sysLocation': '',
  'sysName': '',
  'sysOid': ''
}

describe('Map queries', () =>{
  beforeEach(() => {
    createTestingPinia()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('fetched map devices', () => {
    vi.mock('villus', () => ({
      useQuery: vi.fn().mockImplementation(() => ({
        data: {
          value: {
            listDevices: {
              devices: deviceFixture(mockMapDevice)
            }
          }
        }
      }))
    }))

    const mapQueries = useMapQueries()
    expect(mapQueries.devices).toStrictEqual(deviceFixture(mockMapDevice))
  })
})