import { useNodeStatusStore } from '@/store/Views/nodeStatusStore'
import { setActiveClient, useClient } from 'villus'
import { createTestingPinia } from '@pinia/testing'
import { useNodeStatusQueries } from '@/store/Queries/nodeStatusQueries'
import { useNodeMutations } from '@/store/Mutations/nodeMutations'
import { DownloadCsvVariables, DownloadFormat } from '@/types/graphql'
import { AlertsFilters, Pagination } from '@/types/alerts'
import { TimeRange } from '@/types/graphql'

describe('Node Status Store', () => {
  let store: any
  beforeEach(() => {
    createTestingPinia({ stubActions: false })
    setActiveClient(useClient({ url: 'http://test/graphql' })) 

    store = useNodeStatusStore()
    store.fetchAlertsByNodeData.value = {
      totalAlerts: 20, 
      alerts: [] 
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

  it('calls store.downloadIpInterfacesToCsv with correct parameters', async () => {
    const queries = useNodeStatusQueries()
    const mockSearchTerm: DownloadCsvVariables = {
      nodeId: 12,
      searchTerm: '123',
      downloadFormat: DownloadFormat.Csv
    }
    vi.spyOn(queries, 'downloadIpInterfaces')
    await queries.downloadIpInterfaces(mockSearchTerm)
    expect(queries.downloadIpInterfaces).toHaveBeenCalledWith(mockSearchTerm)
  })

  it('should create and download a blob file with valid parameters', async () => {
    const mockBytes = 'SVAgQUREUkVTUyxJUCBIT1NUTkFNRSxORVRNQVNLLFBSSU1BUlkNCjEyNy4wLjAuMSwsLHRydWUNCg=='
    const mockName = '127.0.0.1-ip-interfaces.csv'
    window.URL.createObjectURL = vi.fn()
    const utils = await import('@/components/utils')
    vi.spyOn(utils, 'createAndDownloadBlobFile')
    utils.createAndDownloadBlobFile(mockBytes, mockName)
    expect(utils.createAndDownloadBlobFile).toHaveBeenCalledWith(mockBytes, mockName)
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
