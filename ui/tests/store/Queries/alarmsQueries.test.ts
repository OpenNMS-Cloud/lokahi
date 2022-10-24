import { createTestingPinia } from '@pinia/testing'
import { useAlarmsQueries } from '@/store/Queries/alarmsQueries'
import { alarmsFixture } from '../../fixture/alarms'

describe('Alarms queries', () =>{
  beforeEach(() => {
    createTestingPinia()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('fetched alarms', () => {
    vi.mock('villus', () => ({
      useQuery: vi.fn().mockImplementation(() => ({
        data: {
          value: {
            listAlarms: {
              alarms: alarmsFixture()
            }
          }
        }
      }))
    }))

    const alarmsQueries = useAlarmsQueries()
    expect(alarmsQueries.alarms).toStrictEqual(alarmsFixture())
  })
})