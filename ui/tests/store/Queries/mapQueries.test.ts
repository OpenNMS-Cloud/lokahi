import { createTestingPinia } from '@pinia/testing'
import { useMapQueries } from '@/store/Queries/mapQueries'
import { nodeFixture } from '../../fixture/nodes'

const mockMapNode: any = {
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
            findAllNodes: nodeFixture(mockMapNode)
          }
        }
      }))
    }))

    const mapQueries = useMapQueries()
    expect(mapQueries.nodes).toStrictEqual(nodeFixture(mockMapNode))
  })
})