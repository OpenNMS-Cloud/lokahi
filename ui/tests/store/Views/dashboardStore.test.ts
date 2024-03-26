import { useDashboardStore } from '@/store/Views/dashboardStore'
import { createTestingPinia } from '@pinia/testing'
import { setActiveClient, useClient } from 'villus'



describe('dashboardStore', () => {

  let store: any
  beforeEach(() => {
    createTestingPinia({ stubActions: false })
    setActiveClient(useClient({ url: 'http://test/graphql' }))

    store = useDashboardStore()

  })

  afterEach(() => {
    vi.restoreAllMocks()

  })

  it('fetches network traffic in metrics', async () => {
    await store.getNetworkTrafficInValues()
    expect(store.getNetworkTrafficInValues).toHaveBeenCalled()
  })

  it('fetches network traffic out metrics', async () => {
    await store.getNetworkTrafficOutValues()
    expect(store.getNetworkTrafficOutValues).toHaveBeenCalled()
  })

  it('fetches top nodes', async () => {
    await store.getTopNNodes({})
    expect(store.getTopNNodes).toHaveBeenCalled()
  })

  it('downloads top nodes as CSV', async () => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
    global.window.atob = (data: string) => {
      return '' as any
    }
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    global.URL.createObjectURL = (o: any) => {
      return 'http://'
    }
    const store = useDashboardStore()
    await store.downloadTopNNodesToCsv()
    expect(store.downloadTopNNodesToCsv).toHaveBeenCalled()
  })

  it('sets top nodes table page', () => {
    const store = useDashboardStore()
    store.setTopNNodesTablePage(2)
    expect(store.topNNodesQueryVariables.page).toBe(2)
  })

  it('sets top nodes table sort', () => {
    const store = useDashboardStore()
    store.setTopNNodesTableSort({ property: 'name', value: 'asc' })
    expect(store.topNNodesQueryVariables.sortBy).toBe('name')
    expect(store.topNNodesQueryVariables.sortAscending).toBe(true)
  })
})
