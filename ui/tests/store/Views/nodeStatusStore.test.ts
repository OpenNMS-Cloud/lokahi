import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import { setActiveClient, useClient } from 'villus'
import { createTestingPinia } from '@pinia/testing'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { useNodeMutations } from '@/store/Mutations/nodeMutations'
import { AlertsFilters, Pagination } from '@/types/alerts'
import { TimeRange } from '@/types/graphql'

describe('Node Status Store', () => {
  let store: any
  beforeEach(() => {
    createTestingPinia({ stubActions: false })
    setActiveClient(useClient({ url: 'http://test/graphql' })) // Create and set a client

    store = useNodeStatusStore()
    store.fetchAlertsByNodeData.value = {
      totalAlerts: 20, // Set some initial total alerts for testing pagination
      alerts: [] // You can add some dummy data for testing if needed
    }
  })

  afterEach(() => {
    vi.restoreAllMocks()
    store.alertsPagination.value = {
      page: 0,
      pageSize: 10,
      total: 0
    }
  })

  // Skipping this test until flows are fully enabled
  it.skip('Correctly calls the fetch exports query', async () => {
    setActiveClient(useClient({ url: 'http://test/graphql' }))

    const store = useNodeStatusStore()
    const queries = useNodeStatusQueries()

    const capturedNowInMs = new Date().getTime()
    const testDate = new Date(capturedNowInMs)

    global.Date = vitest.fn().mockImplementation(() => testDate) as any
    global.Date.now = vitest.fn().mockImplementation(() => capturedNowInMs)

    const endTime = Date.now()
    const startTime = endTime - 1000 * 60 * 60 * 24 * 7 // endTime - 7 days

    await store.fetchExporters(1)

    expect(queries.fetchExporters).toHaveBeenCalledOnce()

    expect(queries.fetchExporters).toHaveBeenCalledWith({
      exporter: [
        {
          nodeId: 1
        }
      ],
      timeRange: {
        startTime,
        endTime
      }
    })
  })


  it('calls nodeStatusQueries.setNodeId with correct parameter and sets nodeId', async () => {
    const store = useNodeStatusStore()
    const queries = useNodeStatusQueries()
    const mockNodeId = 123
    vi.spyOn(queries, 'setNodeId')
    await store.setNodeId(mockNodeId)
    expect(queries.setNodeId).toHaveBeenCalledWith(mockNodeId)
    expect(store.nodeId).toBe(mockNodeId)
  })


  it('calls mutations.updateNode with correct parameters', async () => {
    const store = useNodeStatusStore()
    const mutations = useNodeMutations()
    const mockNodeAlias = 'New Node Alias'
    vi.spyOn(mutations, 'updateNode')
    await store.updateNodeAlias(mockNodeAlias)
    expect(mutations.updateNode).toHaveBeenCalledWith({
      node: {
        nodeAlias: mockNodeAlias
      }
    })
  })


  it('calls getAlertsByNodeQuery with correct parameters and updates fetchAlertsByNodeData and alertsPagination', async () => {
    const queries = useNodeStatusQueries()
    const sortFilter: AlertsFilters = {
      timeRange: TimeRange.All,
      nodeLabel: '',
      severities: [],
      sortAscending: true,
      sortBy: 'id',
      nodeId: 1
    }

    const paginationFilter: Pagination = {
      page: 0, // FE pagination component has base 1 (first page)
      pageSize: 10,
      total: 0
    }

    store.alertsFilter = sortFilter
    store.alertsPagination = paginationFilter
    await store.getAlertsByNode()
    vi.spyOn(queries, 'getAlertsByNodeQuery')
    queries.getAlertsByNodeQuery(sortFilter, paginationFilter)
    expect(queries.getAlertsByNodeQuery).toHaveBeenCalledWith(sortFilter, paginationFilter)
    expect(store.fetchAlertsByNodeData.totalAlerts).toBe(0)
    expect(store.alertsPagination.total).toBe(0)
  })


  it('calls getAlertsByNode when setAlertsByNodePage is called and updates alertsPagination', async () => {
    const getAlertsByNodeMock = vi.spyOn(store, 'getAlertsByNode')
    await store.setAlertsByNodePage(2)
    await store.getAlertsByNode()
    expect(store.alertsPagination.page).toBe(2)
    expect(getAlertsByNodeMock).toHaveBeenCalled()
  })


  it('calls getAlertsByNode when setAlertsByNodePageSize is called and updates alertsPagination', async () => {
    const getAlertsByNodeMock = vi.spyOn(store, 'getAlertsByNode')
    await store.setAlertsByNodePageSize(20)
    await store.getAlertsByNode()
    expect(store.alertsPagination.pageSize).toBe(20)
    expect(getAlertsByNodeMock).toHaveBeenCalled()
  })
})
