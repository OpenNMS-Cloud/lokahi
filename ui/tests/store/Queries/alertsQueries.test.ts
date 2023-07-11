import { getAlertsList } from '../../fixture/alerts'
import { TimeRange } from '@/types/graphql'
import { AlertsFilters, Pagination } from '@/types/alerts'
import { createPinia, setActivePinia } from 'pinia'

describe('Alerts queries', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.resetModules()
  })

  it('fetchAlerts sets expected store value', async () => {
    const expectedAlerts = getAlertsList()
    const mockData = {
      value: {
        findAllAlerts: {
          alerts: expectedAlerts,
          totalAlerts: expectedAlerts.length
        }
      }
    }

    vi.doMock('villus', () => ({
      useQuery: vi.fn().mockImplementation(() => ({
        data: mockData,
        execute: vi.fn().mockResolvedValue(mockData),
        isFetching: {
          value: false
        }
      }))
    }))

    const { useAlertsQueries } = await import('@/store/Queries/alertsQueries')
    const alertsQueries = useAlertsQueries()

    const alertsFilters: AlertsFilters = {
      nodeLabel: '', search: '', severities: [], sortAscending: false, sortBy: '', timeRange: TimeRange.All
    }
    const pagination: Pagination = {
      page: 1,
      pageSize: 10,
      total: 0
    }

    await alertsQueries.fetchAlerts(alertsFilters, pagination)
    expect((alertsQueries.fetchAlertsData as any).alerts).toStrictEqual(expectedAlerts)
  })

  it('fetchCountAlerts sets expected store value', async () => {
    const expectedCount = 10
    const mockData = {
      value: {
        countAlerts: {
          count: expectedCount
        }
      }
    }

    vi.doMock('villus', () => ({
      useQuery: vi.fn().mockImplementation(() => ({
        data: mockData,
        execute: vi.fn().mockResolvedValue(mockData),
        isFetching: {
          value: false
        }
      }))
    }))

    const { useAlertsQueries } = await import('@/store/Queries/alertsQueries')
    const alertsQueries = useAlertsQueries()

    await alertsQueries.fetchCountAlerts([], TimeRange.All)
    expect(alertsQueries.fetchCountAlertsData).toStrictEqual(expectedCount)
  })
})
